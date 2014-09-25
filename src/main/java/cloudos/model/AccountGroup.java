package cloudos.model;

import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity @Accessors(chain=true)
public class AccountGroup extends UniquelyNamedEntity<AccountGroup> {

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

}
