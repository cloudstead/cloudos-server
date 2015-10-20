package cloudos.model.support;

import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupInfo;
import cloudos.model.AccountGroupMember;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.NamedEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@Accessors(chain=true) @NoArgsConstructor
public class AccountGroupView implements NamedEntity {

    public static final JavaType searchResultType = SearchResults.jsonType(AccountGroupView.class);

    @Getter @Setter private String name;
    @Getter @Setter private List<String> mirrors;
    @Getter @Setter private AccountGroupInfo info;
    @Getter @Setter private Set<AccountGroupMemberView> members = new HashSet<>();
    @Setter private Integer memberCount = null;

    public AccountGroupView(AccountGroup group) { copy(this, group); }

    public int getMemberCount () { return memberCount != null ? memberCount : members.size(); }

    public AccountGroupView(String groupName) { setName(groupName); }

    public AccountGroupView addMember (Account account) {
        members.add(new AccountGroupMemberView(account));
        return this;
    }

    public AccountGroupView addMember (AccountGroup group) {
        members.add(new AccountGroupMemberView(group));
        return this;
    }

    public AccountGroupView addMembers(List<AccountGroupMember> groupMembers) {
        for (AccountGroupMember m : new ArrayList<>(groupMembers)) {
            members.add(new AccountGroupMemberView(m));
        }
        memberCount = null;
        return this;
    }

    public void addMember(AccountGroupMember m) { members.add(new AccountGroupMemberView(m)); memberCount = null; }

    public void resetMembers() { members = new HashSet<>(); memberCount = null; }

}
