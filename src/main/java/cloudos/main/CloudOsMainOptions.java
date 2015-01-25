package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.main.MainApiOptionsBase;
import org.kohsuke.args4j.Option;

public class CloudOsMainOptions extends MainApiOptionsBase {

    @Override protected String getDefaultApiBaseUri() { return "http://127.0.0.1:3001/api"; }

    public static final String PASSWORD_ENV_VAR = "CLOUDOS_PASS";

    @Override protected String getPasswordEnvVarName() { return PASSWORD_ENV_VAR; }

    public static final String USAGE_POLL_INTERVAL = "Number of seconds to sleep in between status checks. Default is 4.";
    public static final String OPT_POLL_INTERVAL = "-p";
    public static final String LONGOPT_POLL_INTERVAL = "--poll";
    @Option(name=OPT_POLL_INTERVAL, aliases=LONGOPT_POLL_INTERVAL, usage=USAGE_POLL_INTERVAL)
    @Getter @Setter private long pollInterval = 4;

}
