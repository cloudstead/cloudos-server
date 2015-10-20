package cloudos.dao;

import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import cloudos.model.support.AccountGroupRequest;
import cloudos.service.CloudOsLdapService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.InspectCollection;
import org.cobbzilla.wizard.dao.AbstractLdapDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.*;

import static cloudos.model.AccountGroup.ADMIN_GROUP_NAME;
import static cloudos.model.AccountGroup.DEFAULT_GROUP_NAME;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Repository @Slf4j
public class AccountGroupDAO extends AbstractLdapDAO<AccountGroup> {

    @Autowired private AccountDAO accountDAO;
    @Getter @Autowired private CloudOsLdapService ldapService;

    public AccountGroup findDefaultGroup() { return findByName(DEFAULT_GROUP_NAME); }
    public AccountGroup findAdminGroup() { return findByName(ADMIN_GROUP_NAME); }

    public List<AccountGroup> findMirrors(String groupName) {
        return dnTransform(findByField("mirror", groupName));
    }

    private AccountGroup adminGroup() { return getTemplateObject().adminGroup(); }
    private AccountGroup defaultGroup() { return getTemplateObject().defaultGroup(); }

    public AccountGroup addToDefaultGroup(Account account) {
        AccountGroup defaultGroup = findDefaultGroup();
        if (defaultGroup == null) defaultGroup = create(defaultGroup());
        defaultGroup.addMember(account.getDn());
        update(defaultGroup);
        return defaultGroup;
    }

    public AccountGroup addToAdminGroup(Account account) {
        AccountGroup adminGroup = findAdminGroup();
        if (adminGroup == null) adminGroup = create(adminGroup());
        adminGroup.addMember(account.getDn());
        update(adminGroup);
        return adminGroup;
    }

    @Override public void delete(String name) {

        final AccountGroup group = findByName(name);
        if (group == null) return;

        final String groupName = group.getName();
        final List<AccountGroup> mirrors = findMirrors(groupName);
        if (!mirrors.isEmpty()) throw invalidEx("{err.group.mirrorsExist}", "Cannot delete group, mirrors still exist: "+mirrors);

        if (isDefaultGroup(groupName)) {
            throw invalidEx("{err.group.cannotDeleteDefault}", "Cannot delete default group: "+ groupName);
        }

        super.delete(ldapService.groupDN(name));
    }

    public static boolean isDefaultGroup(String groupName) {
        return groupName.equals(DEFAULT_GROUP_NAME) || groupName.equals(ADMIN_GROUP_NAME);
    }

    // create group, add members
    public AccountGroup create(AccountGroupRequest groupRequest, List<String> recipients) {

        if (findByName(groupRequest.getName()) != null) die("create: Group already exists: "+groupRequest.getName());

        final String groupName = groupRequest.getName();
        final AccountGroup group;
        validate(groupRequest, recipients);
        try {
            group = (AccountGroup) new AccountGroup(config())
                    .setDescription(groupRequest.getDescription())
                    .setMirrors(groupRequest.getMirrorList())
                    .setName(groupName);

            List<String> members = buildGroupMemberList(group, recipients, true);
            if (members.isEmpty()) die("create: Group has no members: "+groupRequest.getName());

            members = buildGroupMemberList(group, recipients, false);
            group.setMembers(members);
            return super.create(group);

        } catch (Exception e) {
            // Remove group and members from DB, and entry from LDAP?
            log.error("create: Error creating group: " + e, e);
            return die("Error creating group: "+e, e);
        }
    }

    public AccountGroup update(@Valid AccountGroupRequest request) {

        final String groupName = request.getName();
        final AccountGroup group = findByName(groupName);
        if (group == null) die("Group not found: "+groupName);

        final List<String> recipients = request.getRecipients();

        if (recipients.isEmpty()) {
            log.info("update: No members, deleting group");
            delete(groupName);
            return null;
        }
        if (createsCircularReference(groupName, recipients)) {
            throw invalidEx("{err.group.circularReference}", "group cannot contain a circular reference");
        }

        // update members
        final List<String> members = buildGroupMemberList(group, recipients, false);
        group.setMembers(members);

        // update quota/description and member list in LDAP
        group.setDescription(request.getDescription());
        group.setStorageQuota(request.getStorageQuota());
        group.setMirrors(request.getMirrorList());
        return update(group);
    }

    /**
     * @param group The group to build a member list for.
     * @return a List of members of this group. If this group is a mirror, this also includes (via union) the members of its mirror source groups.
     */
    public List<String> buildGroupMemberList(AccountGroup group) {

        final Set<String> members = new HashSet<>(group.getMembers());

        if (group.hasMirror()) {
            for (String mirror : group.getMirrors()) {
                final AccountGroup source = findByName(mirror);
                if (source == null) {
                    log.warn("Mirror broken: "+mirror+" -> "+group.getName());
                } else {
                    for (String m : source.getMembers()) {
                        if (m.endsWith(config().getGroup_dn())) {
                            // recursively load group
                            members.addAll(buildGroupMemberList(findByDn(m)));
                        } else {
                            log.warn("adding: '"+m+"' (size before="+members.size()+")");
                            members.add(m);
                            log.warn("added: '"+m+"' (size after="+members.size()+")");
                        }
                    }
                }
            }
        }

        return new ArrayList<>(members);
    }

    /**
     * @param group The group to build a member list for.
     * @param recipients The member names that will comprise the list. They must exist in the DB.
     * @param includeMirror If true and the group is a mirror, include members from the mirror source too
     * @return a List of AccountGroupMembers objects
     */
    private List<String> buildGroupMemberList(AccountGroup group, List<String> recipients, boolean includeMirror) {
        final Set<String> members = new HashSet<>();
        for (String recipient : recipients) members.add(findAccountOrGroupDN(recipient));
        if (includeMirror && group.hasMirror()) {
            for (String mirror : group.getMirrors()) {
                final AccountGroup source = findByName(mirror);
                if (source == null) {
                    log.warn("Mirror broken: " + mirror + " -> " + group.getName());
                } else {
                    members.addAll(buildGroupMemberList(source));
                }
            }
        }
        return new ArrayList<>(members);
    }

    public boolean createsCircularReference(String group, List<String> members) {
        final Map<String, List<String>> map = new HashMap<>();
        for (AccountGroupMember m : findAllMembers()) {
            List<String> groupMembers = map.get(m.getGroupDn());
            if (groupMembers == null) {
                groupMembers = new ArrayList<>();
                map.put(m.getGroupDn(), groupMembers);
            }
            groupMembers.add(m.getMemberDn());
        }

        // we want to see what would happen IF this group were added with these members.
        // So add ourselves last, possibly overwriting a previous value
        final String groupDN = ldapService.groupDN(group);
        final List<String> membersDNs = new ArrayList<>();
        for (String member : members) membersDNs.add(findAccountOrGroupDN(member));
        map.put(groupDN, membersDNs);
        return InspectCollection.containsCircularReference(groupDN, map);
    }

    private List<AccountGroupMember> findAllMembers() {
        final List<AccountGroupMember> all = new ArrayList<>();
        for (AccountGroup g : findAll()) {
            // populate members. todo: we can make this more efficient by including the uniqueMember field in ldapsearch
            g = findByDn(g.getDn());
            for (String member : g.getMembers()) {
                all.add(new AccountGroupMember(g.getDn(), member, config()));
            }
        }
        return all;
    }

    private String findAccountOrGroupDN(String recipient) {
        // Is it an account or another group?
        final Account account = accountDAO.findByName(recipient);
        if (account != null) return account.getDn();

        final AccountGroup accountGroup = findByName(recipient);
        if (accountGroup != null) return accountGroup.getDn();

        throw invalidEx("{err.member.notFound}", "group member does not exist: "+recipient);
    }

    @Override protected String formatBound(String bound, String value) {
        if (bound.equals(idField())) {
            return "(" + idField() + "=" + value + ")";
        } else if (bound.equals("mirror")) {
            return "("+bound+"="+value+")";
        } else {
            return notSupported("formatBound: " + bound);
        }
    }

    private void validate(AccountGroupRequest groupRequest, List<String> recipients) {

        if (groupRequest.hasMirror()) {
            for (String mirror : groupRequest.getMirrorList()) {
                AccountGroup source = findByName(mirror);
                if (source == null) {
                    if (mirror.equalsIgnoreCase(defaultGroup().getName())) {
                        create(defaultGroup());
                    } else if (mirror.equalsIgnoreCase(adminGroup().getName())) {
                        create(adminGroup());
                    } else {
                        throw invalidEx("err.group.mirror.invalid");
                    }
                }
            }
        }

        final String groupName = groupRequest.getName();

        if (!groupRequest.hasMirror() && (recipients == null || recipients.isEmpty())) {
            throw invalidEx("{err.group.empty}", "Cannot create an empty non-mirror group: "+groupName);
        }

        if (findByName(groupName) != null) {
            throw invalidEx("{err.name.notUnique}", "group with same name already exists");
        }

        if (accountDAO.findByName(groupName) != null) {
            throw invalidEx("{err.name.isUser}", "user with same name already exists");
        }

        if (createsCircularReference(groupName, recipients)) {
            throw invalidEx("{err.group.circularReference}", "group cannot contain a circular reference");
        }
    }

}
