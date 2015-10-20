package cloudos.main;

import cloudos.server.CloudOsConfiguration;
import cloudos.server.CloudOsServer;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.main.MainApiOptionsBase;
import org.cobbzilla.wizard.model.ldap.LdapContext;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;

public class CloudOsMainOptions extends MainApiOptionsBase {

    @Override protected String getDefaultApiBaseUri() { return "https://"+CommandShell.hostname()+"/api"; }

    public static final String PASSWORD_ENV_VAR = "CLOUDOS_PASS";

    @Override protected String getPasswordEnvVarName() { return PASSWORD_ENV_VAR; }

    public static final String USAGE_POLL_INTERVAL = "Number of seconds to sleep in between status checks. Default is 4.";
    public static final String OPT_POLL_INTERVAL = "-I";
    public static final String LONGOPT_POLL_INTERVAL = "--poll-interval";
    @Option(name=OPT_POLL_INTERVAL, aliases=LONGOPT_POLL_INTERVAL, usage=USAGE_POLL_INTERVAL)
    @Getter @Setter private long pollInterval = 4;

    @Getter(lazy=true) private final LdapContext ldap = initLdap();

    private LdapContext initLdap () {
        final RestServerHarness<CloudOsConfiguration, CloudOsServer> harness = new RestServerHarness<>(CloudOsServer.class);
        final File config = new File(CommandShell.home("cloudos"), ".cloudos.env");
        if (config.exists()) {
            try {
                harness.init(CommandShell.loadShellExports(config));
            } catch (Exception e) {
                die("Error loading env from "+abs(config)+": "+e, e);
            }
        } else {
            harness.init(System.getenv());
        }
        return harness.getConfiguration().getLdap();
    }

}
