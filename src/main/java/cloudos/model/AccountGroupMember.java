package cloudos.model;

import cloudos.dao.AccountGroupMemberType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.ldap.LdapUtil;
import org.cobbzilla.wizard.model.ldap.LdapContext;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Accessors(chain=true) @NoArgsConstructor
@ToString(of={"groupDn", "memberDn"})
@EqualsAndHashCode(of={"groupDn", "memberDn"}, callSuper=false)
public class AccountGroupMember {

    // duplicated from the AccountGroup. OK since names are immutable.
    @Getter @Setter private String groupDn;

    // duplicated from either Account or AccountGroup. OK since names are immutable.
    @Getter @Setter private String memberDn;

    // Clients don't need to set this (it will be ignored). Server populates it depending what 'name' refers to.
    @Enumerated(EnumType.STRING)
    @Getter @Setter public AccountGroupMemberType type;

    public String getGroupName() { return LdapUtil.getFirstDnValue(getGroupDn()); }
    public String getMemberName() { return LdapUtil.getFirstDnValue(getMemberDn()); }

    @JsonIgnore @Transient public boolean isAccount () { return type == AccountGroupMemberType.account; }
    @JsonIgnore @Transient public boolean isGroup   () { return type == AccountGroupMemberType.group; }

    public AccountGroupMember (AccountGroup group, Account member) {
        this.groupDn = group.getName();
        this.memberDn = member.getName();
        this.type = AccountGroupMemberType.account;
    }

    public AccountGroupMember (AccountGroup group, AccountGroup member) {
        this.groupDn = group.getName();
        this.memberDn = member.getName();
        this.type = AccountGroupMemberType.group;
    }

    public AccountGroupMember (String groupDn, String dn, LdapContext context) {
        this.groupDn = groupDn;
        this.memberDn = dn;
        if (dn.startsWith(context.getUser_username()+"=")) {
            type = AccountGroupMemberType.account;
        } else if (dn.startsWith(context.getGroup_name()+"=")) {
            type = AccountGroupMemberType.group;
        } else {
            die("AccountGroupMember: " + dn);
        }
    }
}
