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
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cloudos.resources.MessageConstants.ERR_STORAGE_QUOTA_INVALID;
import static cloudos.resources.MessageConstants.ERR_STORAGE_QUOTA_LENGTH;
import static org.cobbzilla.util.string.StringUtil.BYTES_PATTERN;

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

    public Account (AccountRequest request) { populate(request); }
    public Account (Account other) { populate(other); }
    public Account (String accountName) { setName(accountName); }

    public Account populate(Account account) {
        super.populate(account);
        setPrimaryGroup(account.getPrimaryGroup());
        setStorageQuota(account.getStorageQuota());
        return this;
    }

    // validated at login (against kerberos) and placed into the session. Not stored in DB.
    @Transient @Getter @Setter private String password;
    @JsonIgnore public boolean hasPassword() { return !StringUtil.empty(password); }

    @Pattern(regexp=BYTES_PATTERN, message=ERR_STORAGE_QUOTA_INVALID)
    @Column(length=10) @Size(max=10, message=ERR_STORAGE_QUOTA_LENGTH)
    @Getter @Setter private String storageQuota;

    @Size(max=PRIMARY_GROUP_MAXLEN, message=ERR_PRIMARY_GROUP_LENGTH)
    @Getter @Setter private String primaryGroup;

    // filled out by SessionDAO when it returns lookups
    @Transient
    @Getter @Setter private List<AppRuntimeDetails> availableApps = new ArrayList<>();
}
