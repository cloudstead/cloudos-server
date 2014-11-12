package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;
import org.kohsuke.args4j.Option;

public class CloudOsMainOptions {

    public static final String USAGE_ACCOUNT = "The account name. Required.";
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

    public static final String PASSWORD_ENV_VAR = "CLOUDOS_PASS";
    private String initPassword() {
        final String pass = System.getenv(PASSWORD_ENV_VAR);
        if (StringUtil.empty(pass)) throw new IllegalStateException("No "+PASSWORD_ENV_VAR+" defined in environment");
        return pass;
    }
}
