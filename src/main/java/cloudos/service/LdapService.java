package cloudos.service;

import cloudos.dao.AccountGroupMemberDAO;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.support.AccountRequest;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.system.Command;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.config.LdapConfiguration;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static cloudos.model.auth.AuthenticationException.Problem.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.system.CommandShell.okResult;


@Service @Slf4j
public class LdapService {

    @Autowired private CloudOsConfiguration configuration;
    @Autowired private AccountGroupMemberDAO memberDAO;

    private LdapConfiguration getLdap() { return configuration.getLdap(); }
    private String password() { return getLdap().getPassword(); }

    public String accountDN(String accountName) { return "uid=" + accountName + ",ou=People," + getLdap().getBaseDN(); }
    public String groupDN  (String groupName)   { return "cn="  + groupName   + ",ou=Groups," + getLdap().getBaseDN(); }
    public String adminDN  ()                   { return "cn=admin,"                          + getLdap().getDomain(); }

    public String ldapFilterGroup(String groupName) {
        return empty(groupName) ? "(objectClass=groupOfUniqueNames)" : "(&(objectClass=groupOfUniqueNames)(cn="+groupName+"))";
    }

    public CommandResult createUser(AccountRequest request) {
        final String password = request.getPassword();  // this will die if there's no password in the request, but I
        // don't see a case where we'd ever create an account without
        // one. password creation isn't something that should be
        // handled here, as we'd need to communicate the password to
        // the kerberos service to make sure it gets set there as well
        final String accountName = request.getAccountName();
        final String accountDN = accountDN(accountName);
        String ldif = "dn: " + accountDN + "\n" +
                "objectClass: inetOrgPerson\n" +
                "uid: " + request.getName() + "\n" +
                "sn: " + request.getLastName() + "\n" +
                "givenName: " + request.getFirstName() + "\n" +
                "cn: " + request.getFullName() + "\n" +
                "displayName: " + request.getFullName() + "\n" +
                "mail: " + request.getEmail() + "\n" +
                "userPassword: " + password + "\n";

        CommandResult result = run_ldapadd(ldif);

        if (result.getStderr().contains("Already exists")) {
            if (!request.isAdmin()) {
                throw new SimpleViolationException("{err.createUser.alreadyExists}",
                        "Account already exists", request.getName());
            }
            deleteUser(accountName);
            result = run_ldapadd(ldif);
        }

        if (result.isZeroExitStatus()) {
            // check to see if we've got the cloudos-user group installed. if not, go ahead and create it, then add this
            // user.
            if (!checkForCloudosGroup()) {
                createGroupWithFirstAccount(AccountGroup.defaultGroup(), accountName);

            } else {
                // the group should now exist, add the user.
                addAccountToGroup(AccountGroup.DEFAULT_GROUP_NAME, accountName);
            }
        }

        return result;
    }

    public CommandResult addAccountToGroup(String groupName, String accountName) {
        return addDnToGroup(groupName, accountDN(accountName));
    }

    public CommandResult addGroupToGroup(String groupName, String groupMember) {
        if (groupMember.equals(groupName)) throw new IllegalArgumentException("addGroupToGroup: Group cannot be member of itself: "+groupName);
        return addDnToGroup(groupName, groupDN(groupMember));
    }

    public CommandResult addDnToGroup(String groupName, String dn) {
        final String ldif = "dn: " + groupDN(groupName) + "\n" +
                "changeType: modify\n" +
                "add: uniqueMember\n" +
                "uniqueMember: " + dn + "\n";
        return run_ldapmodify(ldif);
    }

    public void addToGroup(String groupName, AccountGroupMember member) {
        if (member.isAccount()) {
            addAccountToGroup(groupName, member.getMemberName());
        } else if (member.isGroup()) {
            addGroupToGroup(groupName, member.getMemberName());
        } else {
            throw new IllegalArgumentException("addToGroup: invalid member type: "+member.getType());
        }
    }

    public CommandResult removeAccountFromGroup(String groupName, String accountName) {
        return removeDnFromGroup(groupName, accountDN(accountName));
    }

    public CommandResult removeGroupFromGroup(String groupName, String groupMember) {
        return removeDnFromGroup(groupName, groupDN(groupMember));
    }

    private CommandResult removeDnFromGroup(String groupName, String dn) {
        final String ldif = "dn: " + groupDN(groupName) + "\n" +
                "changeType: modify\n" +
                "delete: uniqueMember\n" +
                "uniqueMember: " + dn + "\n";
        return run_ldapmodify(ldif);
    }

    public void removeFromGroup(String groupName, AccountGroupMember member) {
        if (member.isAccount()) {
            removeAccountFromGroup(groupName, member.getMemberName());
        } else if (member.isGroup()) {
            removeGroupFromGroup(groupName, member.getMemberName());
        } else {
            throw new IllegalArgumentException("removeMember: invalid member type: "+member.getType());
        }
    }

    public CommandResult createGroupWithFirstAccount(AccountGroup group, String accountName) {
        final String groupName = group.getName();
        final String ldif = "dn: " + groupDN(groupName) + "\n" +
                "objectClass: groupOfUniqueNames\n" +
                "cn: " + groupName + "\n" +
                "description: " + group.getInfo().getDescription() + "\n" +
                "uniqueMember: " + accountDN(accountName) + "\n";
        return run_ldapadd(ldif);
    }

    public CommandResult createGroupWithMembers(AccountGroup group, List<AccountGroupMember> members) {
        return createGroupWithMembers(group, members.toArray(new AccountGroupMember[members.size()]));
    }

    public CommandResult createGroupWithMembers(AccountGroup group, AccountGroupMember[] members) {

        final String groupName = group.getName();
        String ldif = "dn: " + groupDN(groupName) + "\n" +
                "objectClass: groupOfUniqueNames\n" +
                "cn: " + groupName + "\n" +
                "description: " + group.getInfo().getDescription() + "\n";

        for (AccountGroupMember member: members) {
            if (member.isAccount()) {
                ldif += "uniqueMember: " + accountDN(member.getMemberName()) + "\n";
            } else if (member.isGroup()) {
                ldif += "uniqueMember: " + groupDN(member.getMemberName()) + "\n";
            } else {
                throw new IllegalArgumentException("Invalid member (bad type: "+member.getType()+"): "+member);
            }
        }
        return run_ldapadd(ldif);
    }

    public CommandResult updateGroupInfo(AccountGroup group) {
        final String groupName = group.getName();
        String ldif = "dn: " + groupDN(groupName) + "\n" +
                "changeType: modify\n" +
                "replace: description\n" +
                "description: " + group.getInfo().getDescription() + "\n";
        CommandResult result = run_ldapmodify(ldif, false);
        if (result != null && !result.isZeroExitStatus() && result.getStderr().contains("ldap_modify: No such object")) {
            final AccountGroupMember[] members = mergeLists(group.getMembers(), group.getMirror());
            result = createGroupWithMembers(group, members);
        }
        return okResult(result);
    }

    private AccountGroupMember[] mergeLists(List<AccountGroupMember> members, String mirror) {
        if (empty(mirror)) return (AccountGroupMember[]) members.toArray();
        return (AccountGroupMember[]) ArrayUtil.merge(members, memberDAO.findByGroupName(mirror)).toArray();
    }


    // this method is provided for completeness' sake, but authentication should really go through kerberos
    public void authenticate(String accountName, String password) throws AuthenticationException {
        final CommandLine ldapsearch = ldapSearchCommand(accountName, password).addArgument(accountDN(accountName));
        CommandResult result;
        try {
            result = CommandShell.exec(ldapsearch);
        } catch (Exception e) {
            log.error("error running ldapsearch (" + e.toString()+ "): " + e,e);
            throw new AuthenticationException(BOOTCONFIG_ERROR);
        }

        if (result.getStderr().contains("Invalid credentials")) throw new AuthenticationException(NOT_FOUND);

        okResult(result);
    }

    public CommandLine ldapAdminSearchCommand(String filter) {
        return ldapDnSearchCommand(adminDN(), password()).addArgument(filter);
    }

    public CommandLine ldapSearchCommand(String accountName, String password) {
        return ldapDnSearchCommand(accountDN(accountName), password);
    }

    public CommandLine ldapDnSearchCommand(String dn, String password) {
        return new CommandLine("ldapsearch")
                    .addArgument("-x")
                    .addArgument("-H")
                    .addArgument("ldapi:///")
                    .addArgument("-D")
                    .addArgument(dn)
                    .addArgument("-w")
                    .addArgument(password);
    }

    public void changePassword(String accountName, String oldPassword, String newPassword) throws
            AuthenticationException {
        final CommandLine command = new CommandLine("ldappasswd")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-a").addArgument(oldPassword)
                .addArgument("-s").addArgument(newPassword)
                .addArgument("-D")
                .addArgument(adminDN())
                .addArgument("-w")
                .addArgument(password())
                .addArgument(accountDN(accountName));
        final CommandResult result;
        try{
            result = CommandShell.exec(command);
        } catch (Exception e) {
            die("error running ldappasswd: " + e,e); return;
        }

        if (result.getStderr().contains("unwilling to verify old password")) throw new AuthenticationException(INVALID);
        okResult(result);
    }

    public void adminChangePassword(String accountName, String newPassword) {
        final CommandLine command = new CommandLine("ldappasswd")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-s").addArgument(newPassword)
                .addArgument("-D")
                .addArgument(adminDN())
                .addArgument("-w")
                .addArgument(password())
                .addArgument(accountDN(accountName));
        final CommandResult result;
        try{
            result = CommandShell.exec(command);
        } catch (Exception e) {
            die("error running ldappasswd: " + e, e); return;
        }
        okResult(result);
    }

    private CommandLine ldapDeleteCommand() {
        return new CommandLine("ldapdelete")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument(adminDN())
                .addArgument("-w")
                .addArgument(password());
    }

    public void deleteDN(String dn) {
        final CommandLine command = ldapDeleteCommand().addArgument(dn);
        CommandResult result = null;
        try{
            result = CommandShell.exec(command);
        } catch (Exception e) {
            die("error running ldapdelete: " + e, e);
        }
        okResult(result);
    }

    // NB: this will also delete the kerberos principal for the account
    public void deleteUser(String accountName) { deleteDN(accountDN(accountName)); }

    public void deleteGroup(String groupName) { deleteDN(groupDN(groupName)); }

    private Boolean checkForCloudosGroup() { return groupExists(AccountGroup.DEFAULT_GROUP_NAME); }

    public Boolean groupExists(String groupName) {
        final CommandResult result;
        try {
            result = CommandShell.exec(ldapAdminSearchCommand(ldapFilterGroup(groupName)));
        } catch (Exception e) {
            return die("error running ldapsearch: " + e, e);
        }
        return result.isZeroExitStatus() && result.getStdout().contains("result: 0 Success") && result.getStdout().contains("numEntries: 1");
    }

    private CommandResult run_ldapadd(String input) {
        final CommandLine ldapAddCommand = new CommandLine("ldapadd")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument(adminDN())
                .addArgument("-w")
                .addArgument(password());
        final CommandResult result;
        try {
            result = CommandShell.exec(new Command(ldapAddCommand).setInput(input));
        } catch (Exception e) {
            return die("error running ldapadd: " + e,e);
        }
        return okResult(result);
    }

    private CommandResult run_ldapmodify(String input) {
        return run_ldapmodify(input, true);
    }

    private CommandResult run_ldapmodify(String input, boolean checkOk) {
        final CommandLine modifyLdapCommand = new CommandLine("ldapmodify")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument(adminDN())
                .addArgument("-w")
                .addArgument(password());
        final CommandResult result;
        try {
            result = CommandShell.exec(new Command(modifyLdapCommand).setInput(input));
        } catch (Exception e) {
            return die("error running ldapmodify: " + e, e);
        }
        return checkOk ? okResult(result) : result;
    }

}

