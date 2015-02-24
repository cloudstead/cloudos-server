package cloudos.model;

import cloudos.model.support.AccountGroupRequest;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.wizard.validation.ValidationMessages.translateMessage;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class AccountGroup extends UniquelyNamedEntity {

    public static final String DEFAULT_GROUP_NAME = "cloudos-users";
    public static final String DEFAULT_GROUP_DESCRIPTION = translateMessage("{ldap.defaultGroupDesc}");
    public static final AccountGroupRequest DEFAULT_GROUP_REQUEST
            = new AccountGroupRequest(DEFAULT_GROUP_NAME, DEFAULT_GROUP_DESCRIPTION);

    public static AccountGroup defaultGroup() {
        return new AccountGroup(DEFAULT_GROUP_NAME, DEFAULT_GROUP_DESCRIPTION);
    }

    public AccountGroup (String name) { super(name); }

    public AccountGroup(String name, String description) {
        super(name);
        setDescription(description);
    }

    // jackson helper class
    public static final JavaType searchResultType = SearchResults.jsonType(AccountGroup.class);

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
