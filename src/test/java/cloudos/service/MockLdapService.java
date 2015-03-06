package cloudos.service;

import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.support.AccountRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cloudos.model.auth.AuthenticationException.Problem.NOT_FOUND;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Slf4j
public class MockLdapService extends LdapService {

    private static final CommandResult SUCCESS = new CommandResult(0, "", "");
    @Getter private Map<String, String> passwordMap = new HashMap<>();
    @Getter private Map<String, AccountGroup> groupMap = new HashMap<>();
    @Getter private Map<String, List<AccountGroupMember>> groupMembers = new HashMap<>();

    public String getPassword (String user) { return passwordMap.get(user); }

    @Override
    public CommandResult createUser(AccountRequest request) {
        passwordMap.put(request.getName(), request.getPassword());
        return SUCCESS;
    }

    @Override
    public CommandResult createGroupWithMembers(AccountGroup group, List<AccountGroupMember> members) {
        final String name = group.getName();
        if (groupMap.containsKey(name)) die("already exists: "+name);
        groupMap.put(name, group);
        groupMembers.put(name, members);
        return CommandResult.OK;
    }

    @Override
    public void authenticate(String accountName, String password) throws AuthenticationException {
        String foundPassword = passwordMap.get(accountName);
        if (foundPassword == null || !foundPassword.equals(password)) throw new AuthenticationException(NOT_FOUND);
    }

    @Override
    public void changePassword(String accountName, String oldPassword, String newPassword) throws AuthenticationException {
        authenticate(accountName, oldPassword);
        passwordMap.put(accountName, newPassword);
    }

    @Override
    public void adminChangePassword(String accountName, String newPassword) {
        passwordMap.put(accountName, newPassword);
    }

    @Override public void deleteUser(String accountName) { passwordMap.remove(accountName); }
    @Override public void deleteGroup(String groupName) { groupMap.remove(groupName); }

    @Override
    public void addToGroup(String groupName, AccountGroupMember member) {
        List<AccountGroupMember> members = groupMembers.get(groupName);
        if (members == null) {
            members = new ArrayList<>();
            groupMembers.put(groupName, members);
        }
        members.add(member);
    }

    @Override
    public void removeFromGroup(String groupName, AccountGroupMember member) {
        final List<AccountGroupMember> members = groupMembers.get(groupName);
        if (members == null) return;
        members.remove(member);
    }
}
