package cloudos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.validation.ValidationMessages.translateMessage;

@Entity @NoArgsConstructor @Accessors(chain=true) @ToString
public class AccountGroup extends UniquelyNamedEntity implements Comparable<AccountGroup> {

    // jackson helper class
    public static final JavaType searchResultType = SearchResults.jsonType(AccountGroup.class);

    @Override public int compareTo(AccountGroup other) { return getName().compareTo(other.getName()); }

    public static final String DEFAULT_GROUP_NAME = "cloudos-users";
    public static final String DEFAULT_GROUP_DESCRIPTION = translateMessage("{groups.default.description}");
    public static AccountGroup defaultGroup() { return new AccountGroup(DEFAULT_GROUP_NAME, DEFAULT_GROUP_DESCRIPTION); }

    public static final String ADMIN_GROUP_NAME = "cloudos-admins";
    public static final String ADMIN_GROUP_DESCRIPTION = translateMessage("{groups.admin.description}");
    public static AccountGroup adminGroup() { return new AccountGroup(ADMIN_GROUP_NAME, ADMIN_GROUP_DESCRIPTION); }

    public AccountGroup (String name) { super(name); }

    public AccountGroup(String name, String description) {
        super(name);
        setDescription(description);
    }

    // name of a group to mirror members from
    @Column(length=100)
    @Size(min=2, max=100, message="err.mirror.length")
    @Getter @Setter private String mirror;

    @JsonIgnore public boolean hasMirror() { return !empty(mirror); }

    @Transient @Getter @Setter private List<AccountGroupMember> members;

    public void addMember(AccountGroupMember member) {
        if (members == null) members = new ArrayList<>();
        members.add(member);
    }

    public boolean hasMembers() { return members != null && !members.isEmpty(); }

    @Embedded @Getter @Setter private AccountGroupInfo info;

    public boolean sameInfo(AccountGroupInfo other) {
        if (other == null) return info == null;
        return info != null && info.equals(other);
    }

    public AccountGroup setDescription(String description) {
        if (info == null) info = new AccountGroupInfo();
        info.setDescription(description);
        return this;
    }

    public AccountGroup setStorageQuota(String quota) {
        if (info == null) info = new AccountGroupInfo();
        info.setStorageQuota(quota);
        return this;
    }

}
