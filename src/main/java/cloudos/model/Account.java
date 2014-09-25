package cloudos.model;

import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.CloudOsAccount;
import cloudos.model.support.AccountRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class Account extends AccountBase implements CloudOsAccount, Scrubbable {

    // jackson helper class
    public static final JavaType searchResultType = SearchResults.jsonType(Account.class);
    private static final ScrubbableField[] SCRUBBABLE = new ScrubbableField[]{
            new ScrubbableField(Account.class, "password", String.class)
    };

    // used in search
    public static final String STATUS = "status";
    public static enum Status { active, invited, suspended, admins, non_admins }

    @Override @JsonIgnore public ScrubbableField[] getFieldsToScrub() { return SCRUBBABLE; }

    public static final Comparator<Account> SORT_ACCOUNT_NAME = new Comparator<Account>() {
        @Override public int compare(Account a1, Account a2) {
            return a1 == null ? 1 : a2 == null ? -1 : String.valueOf(a1.getName()).compareTo(String.valueOf(a2.getName()));
        }
    };

    public Account(AccountRequest request) { setAll(request); }
    public Account (Account other) { setAll(other); }
    public Account (String accountName) { setName(accountName); }

    public void setAll(Object thing) { ReflectionUtil.copy(this, thing); }

    // validated at login (against kerberos) and placed into the session. Not stored in DB.
    @Transient
    @Getter @Setter private String password;

    // filled out by SessionDAO when it returns lookups
    @Transient
    @Getter @Setter private List<AppRuntimeDetails> availableApps = new ArrayList<>();
}
