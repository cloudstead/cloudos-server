package cloudos.dao;

import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import cloudos.model.support.AccountGroupRequest;
import cloudos.service.LdapService;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.InspectCollection;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.*;

import static cloudos.model.AccountGroup.ADMIN_GROUP_NAME;
import static cloudos.model.AccountGroup.DEFAULT_GROUP_NAME;

@Repository @Slf4j
public class AccountGroupDAO extends AbstractCRUDDAO<AccountGroup> {

    @Autowired private LdapService ldap;
    @Autowired private AccountDAO accountDAO;
    @Autowired private AccountGroupMemberDAO memberDAO;

    public AccountGroup findByName(String name) { return findByUniqueField("name", name); }

    public AccountGroup findDefaultGroup() { return findByName(DEFAULT_GROUP_NAME); }
    public AccountGroup findAdminGroup() { return findByName(ADMIN_GROUP_NAME); }

    public List<AccountGroup> findMirrors(String groupName) { return findByField("mirror", groupName); }

    public AccountGroupMember addToDefaultGroup(Account account) {
        AccountGroup defaultGroup = findDefaultGroup();
        if (defaultGroup == null) defaultGroup = create(AccountGroup.defaultGroup());
        return memberDAO.createOrUpdate(new AccountGroupMember(defaultGroup, account));
    }

    public AccountGroupMember addToAdminGroup(Account account) {
        AccountGroup adminGroup = findAdminGroup();
        if (adminGroup == null) adminGroup = create(AccountGroup.adminGroup());
        return memberDAO.createOrUpdate(new AccountGroupMember(adminGroup, account));
    }

    @Override public AccountGroup preUpdate(@Valid AccountGroup group) {
        try { ldap.updateGroupInfo(group); } catch (Exception e) {
            log.error("preUpdate: ldap.updateGroupInfo failed: "+e, e);
        }
        return group;
    }

    @Override public void delete(String uuid) {

        final AccountGroup group = findByUuid(uuid);
        if (group == null) return;

        final List<AccountGroup> mirrors = findMirrors(group.getName());
        if (!mirrors.isEmpty()) throw new SimpleViolationException("{err.group.mirrorsExist}", "Cannot delete group, mirrors still exist: "+mirrors);

        try { ldap.deleteGroup(group.getName()); } catch (Exception e) {
            log.error("delete: ldap.deleteGroup failed: "+e, e);
        }

        super.delete(uuid);
    }

    @Override public Object preCreate(@Valid AccountGroup group) {
        if (group.hasMirror()) {
            final AccountGroup source = findByName(group.getMirror());
            if (source == null) throw new SimpleViolationException("err.group.mirror.invalid");
            return source;
        }
        return null;
    }

    public AccountGroup create(AccountGroupRequest groupRequest, List<String> recipients) {
        // create group, add members
        final String groupName = groupRequest.getName();
        final AccountGroup created;
        validate(groupRequest, recipients);
        try {
            created = create((AccountGroup) new AccountGroup()
                    .setInfo(groupRequest.getInfo())
                    .setMirror(groupRequest.getMirror())
                    .setName(groupName));
            final List<AccountGroupMember> members = buildGroupMemberList(created, recipients, true);
            created.setMembers(members);
            Boolean groupExists = null;
            try {
                groupExists = ldap.groupExists(groupName);
            } catch (Exception e) {
                log.warn("create: ldap error, not doing ldap parts: "+e, e);
            }

            for (AccountGroupMember m : members) {
                m.setGroup(created).setUuid(null);
                try { memberDAO.create(m); } catch (Exception e) {
                    log.warn("create: error adding member: "+m);
                }
            }

            // ensure LDAP creation works before writing to our own DB
            if (groupExists != null) {
                if (groupExists) {
                    mergeMembers(groupRequest, created);
                } else {
                    ldap.createGroupWithMembers(created, members);
                }
            }

        } catch (Exception e) {
            // Remove group and members from DB, and entry from LDAP
            log.error("create: Error creating group: "+e, e);
            throw new IllegalStateException("Error creating group: "+e, e);
        }
        return created;
    }

    public AccountGroup update(@Valid AccountGroupRequest request) {

        final String groupName = request.getName();
        final AccountGroup group = findByName(groupName.toLowerCase());
        final List<String> recipients = request.getRecipients();

        if (!recipients.isEmpty()) {
            if (createsCircularReference(groupName, recipients)) {
                throw new SimpleViolationException("{err.group.circularReference}", "group cannot contain a circular reference");
            }

            // update members
            mergeMembers(request, group);
        }

        // update quota/description and member list in LDAP
        group.setInfo(request.getInfo());
        group.setMembers(buildGroupMemberList(group));
        update(group);

        return group;
    }

    /**
     * @param group The group to build a member list for.
     * @return a List of members of this group. If this group is a mirror, this also includes (via union) the members of its mirror source group.
     */
    public List<AccountGroupMember> buildGroupMemberList(AccountGroup group) {

        final Map<String, AccountGroupMember> members = new HashMap<>();
        for (AccountGroupMember m : memberDAO.findByGroup(group.getUuid())) {
            members.put(m.getMemberName(), m);
        }

        if (group.hasMirror()) {
            final AccountGroup source = findByName(group.getMirror());
            if (source == null) {
                log.warn("Mirror broken: "+group.getMirror()+" -> "+group.getName());
            } else {
                for (AccountGroupMember m : memberDAO.findByGroup(source.getUuid())) {
                    members.put(m.getMemberName(), m);
                }
            }
        }

        return new ArrayList<>(members.values());
    }

    /**
     * @param group The group to build a member list for.
     * @param recipients The member names that will comprise the list. They must exist in the DB.
     * @param includeMirror If true and the group is a mirror, include members from the mirror source too
     * @return a List of AccountGroupMembers objects
     */
    private List<AccountGroupMember> buildGroupMemberList(AccountGroup group, List<String> recipients, boolean includeMirror) {
        final Set<AccountGroupMember> members = new HashSet<>();
        for (String recipient : recipients) members.add(populateByName(group, recipient));
        if (includeMirror && group.hasMirror()) {
            final AccountGroup source = findByName(group.getMirror());
            if (source == null) {
                log.warn("Mirror broken: "+group.getMirror()+" -> "+group.getName());
            } else {
                members.addAll(buildGroupMemberList(source));
            }
        }
        return new ArrayList<>(members);
    }

    public boolean createsCircularReference(String group, List<String> members) {
        final Map<String, List<String>> map = new HashMap<>();
        for (AccountGroupMember m : memberDAO.findAll()) {
            List<String> groupMembers = map.get(m.getGroupName());
            if (groupMembers == null) {
                groupMembers = new ArrayList<>();
                map.put(m.getGroupName(), groupMembers);
            }
            groupMembers.add(m.getMemberName());
        }

        // we want to see what would happen IF this group were added with these members.
        // So add ourselves last, possibly overwriting a previous value
        map.put(group, members);
        return InspectCollection.containsCircularReference(group, map);
    }

    private AccountGroupMember populateByName(AccountGroup group, String recipient) {
        // Is it an account or another group?
        final Account account = accountDAO.findByName(recipient);
        if (account != null) return new AccountGroupMember(group, account);

        final AccountGroup accountGroup = findByName(recipient);
        if (accountGroup != null) return new AccountGroupMember(group, accountGroup);

        throw new SimpleViolationException("{err.member.notFound}", "group member does not exist");
    }

    private AccountGroupMember populateByUuid(AccountGroup group, String uuid) {
        // Is it an account or another group?
        final Account account = accountDAO.findByUuid(uuid);
        if (account != null) return new AccountGroupMember(group, account);

        final AccountGroup accountGroup = findByUuid(uuid);
        if (accountGroup != null) return new AccountGroupMember(group, accountGroup);

        throw new SimpleViolationException("{err.member.notFound}", "group member does not exist");
    }

    private void validate(AccountGroupRequest groupRequest, List<String> recipients) {

        if (groupRequest.hasMirror() && findByName(groupRequest.getMirror()) == null) {
            throw new SimpleViolationException("{err.group.mirror.invalid}", "mirror source does not exist: "+groupRequest.getMirror());
        }

        final String groupName = groupRequest.getName();

        if (!groupRequest.hasMirror() && (recipients == null || recipients.isEmpty())) {
            throw new SimpleViolationException("{err.group.empty}", "Cannot create an empty non-mirror group: "+groupName);
        }

        if (findByName(groupName) != null) {
            throw new SimpleViolationException("{err.name.notUnique}", "group with same name already exists");
        }

        if (accountDAO.findByName(groupName) != null) {
            throw new SimpleViolationException("{err.name.isUser}", "user with same name already exists");
        }

        if (createsCircularReference(groupName, recipients)) {
            throw new SimpleViolationException("{err.group.circularReference}", "group cannot contain a circular reference");
        }
    }

    public void mergeMembers(AccountGroupRequest groupRequest, AccountGroup group) {
        addMembers(group, removeMembers(groupRequest, group));
        for (AccountGroup mirror : findMirrors(group.getName())) {
            groupRequest.setName(mirror.getName()); // just in case anyone cares later on
            addMembers(mirror, removeMembers(groupRequest, mirror));
        }
    }

    /**
     * Removes members from a group. The recipients list of the groupRequest is checked against the database.
     * @param groupRequest Any names not present in the recipients list but found in the DB will be removed from the DB and from LDAP.
     * @param group The group (from DB with proper UUID)
     * @return A list of names in the recipient list that were not found in the DB. These can later be added to the group.
     */
    public ArrayList<String> removeMembers(AccountGroupRequest groupRequest, AccountGroup group) {
        // remove members in DB that are not in the request, and determine which members need to be added
        final ArrayList<String> newMembers = new ArrayList<>(groupRequest.getRecipients());

        // find current members
        final List<AccountGroupMember> members = memberDAO.findByGroup(group.getUuid());

        for (AccountGroupMember m : members) {
            if (!groupRequest.getRecipients().contains(m.getMemberName())) {
                removeMember(group.getName(), m);
            }
            newMembers.remove(m.getMemberName()); // already a member, don't need to re-add
        }
        return newMembers;
    }

    private void addMembers(AccountGroup group, ArrayList<String> newMembers) {
        final List<AccountGroupMember> toAdd = buildGroupMemberList(group, newMembers, false);
        for (AccountGroupMember m : toAdd) {
            addMember(group, m);
        }
    }

    private void addMember(AccountGroup group, AccountGroupMember m) {
        try { ldap.addToGroup(group.getName(), m); } catch (Exception e) {
            log.error("ldap.addToGroup failed: "+e, e);
        }
        memberDAO.create(m.setGroupUuid(group.getUuid()));
    }

    private void removeMember(String groupName, AccountGroupMember m) {
        try { ldap.removeFromGroup(groupName, m); } catch (Exception e) {
            log.error("ldap.removeFromGroup failed: "+e, e);
        }
        memberDAO.delete(m.getUuid());
    }

}
