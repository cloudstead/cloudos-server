package cloudos.service;

import cloudos.model.support.AccountRequest;
import cloudos.model.auth.AuthenticationException;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.eclipse.jetty.server.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import static cloudos.model.auth.AuthenticationException.Problem.BOOTCONFIG_ERROR;
import static cloudos.model.auth.AuthenticationException.Problem.INVALID;
import static cloudos.model.auth.AuthenticationException.Problem.NOT_FOUND;


@Service @Slf4j
public class LdapService {

    @Autowired private CloudOsConfiguration configuration;

    public CommandResult createUser(AccountRequest request) {
        final String password = request.getPassword();  // this will die if there's no password in the request, but I
                                                        // don't see a case where we'd ever create an account without
                                                        // one. password creation isn't something that should be
                                                        // handled here, as we'd need to communicate the password to
                                                        // the kerberos service to make sure it gets set there as well
        final String accountDN = getAccountDN(request.getAccountName());
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
                throw new SimpleViolationException("{error.createAccount.alreadyExists}",
                        "Account already exists", request.getName());
            }
            deleteUser(request.getAccountName());
            result = run_ldapadd(ldif);
        }

        if (result.isZeroExitStatus()) {
            // check to see if we've got the cloudos-user group installed. if not, go ahead and create it, then add this
            // user.
            if (!checkForCloudosGroup()) {
                ldif= "dn: cn=cloudos-users,ou=Groups," + configuration.getLdapBaseDN() + "\n" +
                        "objectClass: groupOfUniqueNames\n" +
                        "cn: cloudos-users\n" +
                        "description: CloudOS Users\n" +
                        "uniqueMember: " + accountDN + "\n";
                CommandResult groupCreateResult = run_ldapadd(ldif);
            } else {
                // if the group does exist, add the user.
                ldif= "dn: cn=cloudos-users,ou=Groups," + configuration.getLdapBaseDN() + "\n" +
                        "changeType: modify\n" +
                        "add: uniqueMember\n" +
                        "uniqueMember: " + accountDN + "\n";
                CommandResult groupAddResult = run_ldapmodify(ldif);
            }
        }

        return result;
    }

    // this method is provided for completeness' sake, but authentication should really go through kerberos
    public void authenticate(String accountName, String password) throws AuthenticationException {
        CommandLine ldapsearch = new CommandLine("ldapsearch")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument(getAccountDN(accountName))
                .addArgument("-w")
                .addArgument(password);
        CommandResult result;
        try {
            result = CommandShell.exec(ldapsearch);
        } catch (Exception e) {
            log.error("error running ldapsearch (" + e.toString()+ "): " + e,e);
            throw new AuthenticationException(BOOTCONFIG_ERROR);
        }

        if (result.getStderr().contains("Invalid credentials")) throw new AuthenticationException(NOT_FOUND);
    }

    public void changePassword(String accountName, String oldPassword, String newPassword) throws
            AuthenticationException {
        final CommandLine command = new CommandLine("ldappasswd")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-A")
                .addArgument("-S")
                .addArgument("-D")
                .addArgument("cn=admin," + configuration.getLdapDomain())
                .addArgument("-w")
                .addArgument(configuration.getLdapPassword())
                .addArgument(getAccountDN(accountName));
        final CommandResult result;
        try{
            result = CommandShell.exec(command,
                    oldPassword + "\n" +
                    oldPassword + "\n" +
                    newPassword + "\n" +
                    newPassword + "\n");
        } catch (Exception e) {
            throw new IllegalStateException("error running ldappasswd: " + e,e);
        }

        if (result.getStderr().contains("unwilling to verify old password")) throw new AuthenticationException(INVALID);

    }

    public void adminChangePassword(String accountName, String newPassword) {
        final CommandLine command = new CommandLine("ldappasswd")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-S")
                .addArgument("-D")
                .addArgument("cn=admin," + configuration.getLdapDomain())
                .addArgument("-w")
                .addArgument(configuration.getLdapPassword())
                .addArgument(getAccountDN(accountName));
        final CommandResult result;
        try{
            result = CommandShell.exec(command,
                            newPassword + "\n" +
                            newPassword + "\n" );
        } catch (Exception e) {
            throw new IllegalStateException("error running ldappasswd: " + e,e);
        }
    }

    // NB: this will also delete the kerberos principal for the account
    public void deleteUser(String accountName) {
        final CommandLine command = new CommandLine("ldapdelete")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument("cn=admin," + configuration.getLdapDomain())
                .addArgument("-w")
                .addArgument(configuration.getLdapPassword())
                .addArgument(getAccountDN(accountName));
        final CommandResult result;
        try{
            result = CommandShell.exec(command);
        } catch (Exception e) {
            throw new IllegalStateException("error running ldapdelete: " + e,e);
        }

    }

    private Boolean checkForCloudosGroup() {
        final CommandLine checkForGroupCommand = new CommandLine("ldapsearch")
                .addArgument("-Q")
                .addArgument("-Y")
                .addArgument("EXTERNAL")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-b")
                .addArgument("ou=Groups," + configuration.getLdapBaseDN())
                .addArgument("dn");
        final CommandResult result;
        try {
            result = CommandShell.exec(checkForGroupCommand);
        } catch (Exception e) {
            throw new IllegalStateException("error running ldapsearch: " + e, e);
        }
        return (result.getStdout().contains("result: 0 Success"));
    }

    private String getAccountDN(String accountName) {
        return "uid=" + accountName + ",ou=People," + configuration.getLdapBaseDN();
    }

    private CommandResult run_ldapadd(String input) {
        final CommandLine ldapAddCommand = new CommandLine("ldapadd")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument("cn=admin," + configuration.getLdapDomain())
                .addArgument("-w")
                .addArgument(configuration.getLdapPassword());
        final CommandResult result;
        try {
            result = CommandShell.exec(ldapAddCommand, input);
        } catch (Exception e) {
            throw new IllegalStateException("error running ldapadd: " + e,e);
        }

        if (!result.isZeroExitStatus()) {
            log.error("ldapadd returned non-zero: "+result.getExitStatus());
            throw new IllegalArgumentException(result.getStderr());
        }

        return result;
    }

    private CommandResult run_ldapmodify(String input) {
        final CommandLine createLdapCommand = new CommandLine("ldapmodify")
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldapi:///")
                .addArgument("-D")
                .addArgument("cn=admin," + configuration.getLdapDomain())
                .addArgument("-w")
                .addArgument(configuration.getLdapPassword());
        final CommandResult result;
        try {
            result = CommandShell.exec(createLdapCommand, input);
        } catch (Exception e) {
            throw new IllegalStateException("error running ldapmodify: " + e, e);
        }

        if (!result.isZeroExitStatus()) {
            log.error("ldapmodify returned non-zero: "+result.getExitStatus());
            throw new IllegalArgumentException(result.getStderr());
        }

        return result;
    }
}

