package cloudos.model;

import cloudos.dao.AccountGroupMemberType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.*;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.UUID_MAXLEN;

@Entity @Accessors(chain=true) @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"groupUuid", "memberUuid"}))
@EqualsAndHashCode(of={"groupUuid", "memberUuid"}, callSuper=false)
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

    public AccountGroupMember (AccountGroup group, Account member) {
        this.groupUuid = group.getUuid();
        this.groupName = group.getName();
        this.memberUuid = member.getUuid();
        this.memberName = member.getAccountName();
        this.type = AccountGroupMemberType.ACCOUNT;
    }

    public AccountGroupMember (AccountGroup group, AccountGroup member) {
        this.groupUuid = group.getUuid();
        this.groupName = group.getName();
        this.memberUuid = member.getUuid();
        this.memberName = member.getName();
        this.type = AccountGroupMemberType.GROUP;
    }

}
