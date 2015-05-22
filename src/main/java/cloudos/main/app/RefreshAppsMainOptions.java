package cloudos.main.app;

import cloudos.main.CloudOsMainOptions;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class RefreshAppsMainOptions extends CloudOsMainOptions {

    @Override protected boolean requireAccount() { return false; }

    public static final String USAGE_KEY = "The refresh key. Required if no account provided";
    public static final String OPT_KEY = "-k";
    public static final String LONGOPT_KEY = "--key";
    @Option(name=OPT_KEY, aliases=LONGOPT_KEY, usage=USAGE_KEY)
    @Getter @Setter private String refreshKey;

    public boolean hasRefreshKey () { return !empty(refreshKey); }

}
