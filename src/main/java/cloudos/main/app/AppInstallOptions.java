package cloudos.main.app;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class AppInstallOptions extends AppVersionOptions {

    public static final String USAGE_FORCE = "Force the install, even if the same version is installed.";
    public static final String OPT_FORCE = "-f";
    public static final String LONGOPT_FORCE = "--force";
    @Option(name=OPT_FORCE, aliases=LONGOPT_FORCE, usage=USAGE_FORCE)
    @Getter @Setter private boolean force;

    public String getForceParam() { return force ? "?force=true" : ""; }

}
