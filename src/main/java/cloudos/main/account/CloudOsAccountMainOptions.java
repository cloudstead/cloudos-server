package cloudos.main.account;

import cloudos.main.CloudOsMainOptions;
import cloudos.model.support.AccountRequest;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.wizard.api.CrudOperation;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class CloudOsAccountMainOptions extends CloudOsMainOptions {

    public static final String USAGE_NAME = "Name of the account (if omitted all accounts will be listed). Required for write operations.";
    public static final String OPT_NAME = "-n";
    public static final String LONGOPT_NAME = "--name";
    @Option(name=OPT_NAME, aliases=LONGOPT_NAME, usage=USAGE_NAME)
    @Getter @Setter private String name;
    public boolean hasName () { return !empty(name); }

    public static final String USAGE_QUOTA = "Storage quota for the group";
    public static final String OPT_QUOTA = "-q";
    public static final String LONGOPT_QUOTA = "--quota";
    @Option(name=OPT_QUOTA, aliases=LONGOPT_QUOTA, usage=USAGE_QUOTA)
    @Getter @Setter private String quota;

    public static final String USAGE_OPERATION = "The operation to perform";
    public static final String OPT_OPERATION = "-o";
    public static final String LONGOPT_OPERATION = "--operation";
    @Option(name=OPT_OPERATION, aliases=LONGOPT_OPERATION, usage=USAGE_OPERATION)
    @Getter @Setter private CrudOperation operation = CrudOperation.read;

    public static final String USAGE_ADMIN = "Make user an admin";
    public static final String OPT_ADMIN = "-A";
    public static final String LONGOPT_ADMIN = "--admin";
    @Option(name=OPT_ADMIN, aliases=LONGOPT_ADMIN, usage=USAGE_ADMIN)
    @Getter @Setter private boolean admin = false;

    public static final String USAGE_EMAIL = "Recovery email for the account";
    public static final String OPT_EMAIL = "-e";
    public static final String LONGOPT_EMAIL = "--email";
    @Option(name=OPT_EMAIL, aliases=LONGOPT_EMAIL, usage=USAGE_EMAIL)
    @Getter @Setter private String email;

    public static final String USAGE_ACCOUNT_PASSWORD = "Password for the new account";
    public static final String OPT_ACCOUNT_PASSWORD = "-P";
    public static final String LONGOPT_ACCOUNT_PASSWORD = "--account-password";
    @Option(name=OPT_ACCOUNT_PASSWORD, aliases=LONGOPT_ACCOUNT_PASSWORD, usage=USAGE_ACCOUNT_PASSWORD)
    @Getter @Setter private String accountPassword;

    public static final String USAGE_FIRSTNAME = "First name of the account";
    public static final String OPT_FIRSTNAME = "-f";
    public static final String LONGOPT_FIRSTNAME = "--firstname";
    @Option(name=OPT_FIRSTNAME, aliases=LONGOPT_FIRSTNAME, usage=USAGE_FIRSTNAME)
    @Getter @Setter private String firstname;

    public static final String USAGE_LASTNAME = "Last name of the account";
    public static final String OPT_LASTNAME = "-l";
    public static final String LONGOPT_LASTNAME = "--lastname";
    @Option(name=OPT_LASTNAME, aliases=LONGOPT_LASTNAME, usage=USAGE_LASTNAME)
    @Getter @Setter private String lastname;

    public static final String USAGE_COUNTRYCODE = "Mobile phone country code";
    public static final String OPT_COUNTRYCODE = "-c";
    public static final String LONGOPT_COUNTRYCODE = "--countrycode";
    @Option(name=OPT_COUNTRYCODE, aliases=LONGOPT_COUNTRYCODE, usage=USAGE_COUNTRYCODE)
    @Getter @Setter private int countrycode;

    public static final String USAGE_MOBILEPHONE = "Mobile phone number";
    public static final String OPT_MOBILEPHONE = "-m";
    public static final String LONGOPT_MOBILEPHONE = "--mobilephone";
    @Option(name=OPT_MOBILEPHONE, aliases=LONGOPT_MOBILEPHONE, usage=USAGE_MOBILEPHONE)
    @Getter @Setter private String mobilephone;

    public static final String USAGE_TWOFACTOR = "Enable two-factor authentication for the account";
    public static final String OPT_TWOFACTOR = "-T";
    public static final String LONGOPT_TWOFACTOR = "--twofactor";
    @Option(name=OPT_TWOFACTOR, aliases=LONGOPT_TWOFACTOR, usage=USAGE_TWOFACTOR)
    @Getter @Setter private boolean twofactor = false;

    public static final String USAGE_SUSPENDED = "Suspend account";
    public static final String OPT_SUSPENDED = "-S";
    public static final String LONGOPT_SUSPENDED = "--suspend";
    @Option(name=OPT_SUSPENDED, aliases=LONGOPT_SUSPENDED, usage=USAGE_SUSPENDED)
    @Getter @Setter private boolean suspended = false;

    public AccountRequest getAccountRequest() {
        return (AccountRequest) new AccountRequest()
                .setPassword(getAccountPassword())
                .setStorageQuota(getQuota())
                .setAdmin(isAdmin())
                .setEmail(getEmail())
                .setFirstName(getFirstname())
                .setLastName(getLastname())
                .setMobilePhoneCountryCode(getCountrycode())
                .setMobilePhone(getMobilephone())
                .setSuspended(isSuspended())
                .setTwoFactor(isTwofactor())
                .setName(getName());
    }
}
