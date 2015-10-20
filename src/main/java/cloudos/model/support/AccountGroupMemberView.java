package cloudos.model.support;

import cloudos.dao.AccountGroupMemberType;
import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class AccountGroupMemberView {

    @Getter @Setter private String name;
    @Getter @Setter private AccountGroupMemberType type;

    public AccountGroupMemberView(Account account) {
        this.name = account.getName();
        this.type = AccountGroupMemberType.account;
    }

    public AccountGroupMemberView(AccountGroup group) {
        this.name = group.getName();
        this.type = AccountGroupMemberType.group;
    }

    public AccountGroupMemberView(AccountGroupMember m) {
        this.name = m.getMemberName();
        this.type = m.getType();
    }
}
