package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;
import org.kohsuke.args4j.Option;

import java.util.concurrent.TimeUnit;

public class CloudOsMainOptions {

    public static final String PASSWORD_ENV_VAR = "CLOUDOS_PASS";

    public static final String USAGE_ACCOUNT = "The account name. Required. The password must be in the "+PASSWORD_ENV_VAR+" environment variable";
    public static final String OPT_ACCOUNT = "-a";
    public static final String LONGOPT_ACCOUNT = "--account";
    @Option(name=OPT_ACCOUNT, aliases=LONGOPT_ACCOUNT, usage=USAGE_ACCOUNT, required=true)
    @Getter @Setter private String account;

    public static final String USAGE_API_BASE = "The server's API base URI. Required.";
    public static final String OPT_API_BASE = "-s";
    public static final String LONGOPT_API_BASE = "--server";
    @Option(name=OPT_API_BASE, aliases=LONGOPT_API_BASE, usage=USAGE_API_BASE, required=true)
    @Getter @Setter private String apiBase;

    @Getter private final String password = initPassword();

    private String initPassword() {
        final String pass = System.getenv(PASSWORD_ENV_VAR);
        if (StringUtil.empty(pass)) throw new IllegalStateException("No "+PASSWORD_ENV_VAR+" defined in environment");
        return pass;
    }

    public static final String USAGE_POLL_INTERVAL = "Number of seconds to sleep in between status checks. Default is 4.";
    public static final String OPT_POLL_INTERVAL = "-p";
    public static final String LONGOPT_POLL_INTERVAL = "--poll";
    @Option(name=OPT_POLL_INTERVAL, aliases=LONGOPT_POLL_INTERVAL, usage=USAGE_POLL_INTERVAL)
    @Getter @Setter private long pollInterval = TimeUnit.SECONDS.toMillis(4);

}
