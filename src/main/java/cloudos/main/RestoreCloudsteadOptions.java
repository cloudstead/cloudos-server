package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class RestoreCloudsteadOptions extends CloudOsMainOptions {

    @Override protected boolean requireAccount() { return false; }

    public static final String USAGE_KEY = "The base64-encoded restore key";
    public static final String OPT_KEY = "-k";
    public static final String LONGOPT_KEY = "--key";
    @Option(name=OPT_KEY, aliases=LONGOPT_KEY, usage=USAGE_KEY, required=true)
    @Getter @Setter private String key;

    public static final String USAGE_PASSWORD = "The cloudstead initial password";
    public static final String OPT_PASSWORD = "-w";
    public static final String LONGOPT_PASSWORD = "--password";
    @Option(name=OPT_PASSWORD, aliases=LONGOPT_PASSWORD, usage=USAGE_PASSWORD, required=true)
    @Getter @Setter private String password;

    public static final String USAGE_NOTIFY_EMAIL = "The email address to receive a notification upon completion";
    public static final String OPT_NOTIFY_EMAIL = "-n";
    public static final String LONGOPT_NOTIFY_EMAIL = "--notify-email";
    @Option(name=OPT_NOTIFY_EMAIL, aliases=LONGOPT_NOTIFY_EMAIL, usage=USAGE_NOTIFY_EMAIL, required=true)
    @Getter @Setter private String notifyEmail;

}
