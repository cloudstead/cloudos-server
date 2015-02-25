package cloudos.model;

import cloudos.dao.AccountGroupMemberType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.*;
import javax.validation.constraints.Size;

@Entity @Accessors(chain=true) @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"groupName", "memberName"}))
@EqualsAndHashCode(of={"groupName", "memberName"}, callSuper=false)
public class AccountGroupMember extends IdentifiableBase {

    // we are a member of the alias below
    // Clients pass in an group name, server-side this is changed into a UUID (and back to name when returning values to client)
    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Size(max=UUID_MAXLEN)
    @Getter @Setter private String groupUuid;

    // duplicated from the AccountGroup. OK since names are immutable.
    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String groupName;

    // this is our uuid -- might be an Account or an AccountGroup
    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Size(max=UUID_MAXLEN)
    @Getter @Setter private String memberUuid;

    // duplicated from either Account or AccountGroup. OK since names are immutable.
    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String memberName;

    // Clients don't need to set this (it will be ignored). Server populates it depending what 'name' refers to.
    @Enumerated(EnumType.STRING)
    @Getter @Setter public AccountGroupMemberType type;

    @JsonIgnore @Transient public boolean isAccount () { return type == AccountGroupMemberType.account; }
    @JsonIgnore @Transient public boolean isGroup   () { return type == AccountGroupMemberType.group; }

    public AccountGroupMember (AccountGroup group, Account member) {
        this.groupUuid = group.getUuid();
        this.groupName = group.getName();
        this.memberUuid = member.getUuid();
        this.memberName = member.getAccountName();
        this.type = AccountGroupMemberType.account;
    }

    public AccountGroupMember (AccountGroup group, AccountGroup member) {
        this.groupUuid = group.getUuid();
        this.groupName = group.getName();
        this.memberUuid = member.getUuid();
        this.memberName = member.getName();
        this.type = AccountGroupMemberType.group;
    }

    public AccountGroupMember setGroup(AccountGroup group) {
        setGroupUuid(group.getUuid());
        setGroupName(group.getName());
        return this;
    }
}
