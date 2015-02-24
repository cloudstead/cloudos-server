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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cloudos.model.AccountGroup.DEFAULT_GROUP_NAME;

@Repository @Slf4j
public class AccountGroupDAO extends AbstractCRUDDAO<AccountGroup> {

    @Autowired private LdapService ldap;
    @Autowired private AccountDAO accountDAO;
    @Autowired private AccountGroupMemberDAO memberDAO;

    public AccountGroup findByName(String name) { return findByUniqueField("name", name); }

    public AccountGroup defaultGroup() { return findByName(DEFAULT_GROUP_NAME); }

    public AccountGroupMember addToDefaultGroup(Account account) {
        AccountGroup defaultGroup = defaultGroup();
        if (defaultGroup == null) defaultGroup = create(AccountGroup.defaultGroup());
        return memberDAO.createOrUpdate(new AccountGroupMember(defaultGroup, account));
    }

    @Override public AccountGroup preUpdate(@Valid AccountGroup group) {
        try { ldap.updateGroupInfo(group); } catch (Exception e) {
            log.error("preUpdate: ldap.updateGroupInfo failed: "+e, e);
        }
        return group;
    }

    @Override public void delete(String uuid) {
        try {
            final AccountGroup group = findByUuid(uuid);
            if (group != null) ldap.deleteGroup(group.getName());
        } catch (Exception e) {
            log.error("delete: ldap.deleteGroup failed: "+e, e);
        }
        super.delete(uuid);
    }

    public AccountGroup create(AccountGroupRequest groupRequest, List<String> recipients) {
        // create group, add members
        final String groupName = groupRequest.getName();
        final AccountGroup created;
        validate(groupName, recipients);
        try {
            created = create((AccountGroup) new AccountGroup()
                    .setInfo(groupRequest.getInfo())
                    .setName(groupName));
            final List<AccountGroupMember> members = buildGroupMemberList(created, recipients);
            created.setMembers(members);
            Boolean groupExists = null;
            try {
                groupExists = ldap.groupExists(groupName);
            } catch (Exception e) {
                log.warn("ldap error, not doing ldap parts: "+e, e);
            }

            for (AccountGroupMember m : members) memberDAO.create(m.setGroupUuid(created.getUuid()));

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

    public List<AccountGroupMember> buildGroupMemberList(AccountGroup group) {
        final List<AccountGroupMember> members = new ArrayList<>();
        for (AccountGroupMember m : memberDAO.findByGroup(group.getUuid())) members.add(populateByUuid(group, m.getMemberUuid()));
        return members;
    }

    public List<AccountGroupMember> buildGroupMemberList(AccountGroup group, List<String> recipients) {
        final List<AccountGroupMember> members = new ArrayList<>();
        for (String recipient : recipients) members.add(populateByName(group, recipient));
        return members;
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

    public AccountGroupMember populateByName(AccountGroup group, String recipient) {
        // Is it an account or another group?
        final Account account = accountDAO.findByName(recipient);
        if (account != null) return new AccountGroupMember(group, account);

        final AccountGroup accountGroup = findByName(recipient);
        if (accountGroup != null) return new AccountGroupMember(group, accountGroup);

        throw new SimpleViolationException("{err.member.notFound}", "group member does not exist");
    }

    public AccountGroupMember populateByUuid(AccountGroup group, String uuid) {
        // Is it an account or another group?
        final Account account = accountDAO.findByUuid(uuid);
        if (account != null) return new AccountGroupMember(group, account);

        final AccountGroup accountGroup = findByUuid(uuid);
        if (accountGroup != null) return new AccountGroupMember(group, accountGroup);

        throw new SimpleViolationException("{err.member.notFound}", "group member does not exist");
    }

    public void validate(String groupName, List<String> recipients) {
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

        final String groupName = groupRequest.getName();

        // find current members
        final List<AccountGroupMember> members = memberDAO.findByGroup(group.getUuid());

        // remove members in DB that are not in the request, and determine which members need to be added
        final ArrayList<String> newMembers = new ArrayList<>(groupRequest.getRecipients());
        for (AccountGroupMember m : members) {
            if (!groupRequest.getRecipients().contains(m.getMemberName())) {
                try { ldap.removeFromGroup(groupName, m); } catch (Exception e) {
                    log.error("mergeMembers: ldap.removeFromGroup failed: "+e, e);
                }
                memberDAO.delete(m.getUuid());
            }
            newMembers.remove(m.getMemberName()); // already a member, don't need to re-add
        }

        // update members
        final List<AccountGroupMember> toAdd = buildGroupMemberList(group, newMembers);
        for (AccountGroupMember m : toAdd) {
            try { ldap.addToGroup(groupName, m); } catch (Exception e) {
                log.error("mergeMembers: ldap.addToGroup failed: "+e, e);
            }
            memberDAO.create(m.setGroupUuid(group.getUuid()));
        }
    }

}
