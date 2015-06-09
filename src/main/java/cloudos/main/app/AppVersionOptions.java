package cloudos.main.app;

import cloudos.main.CloudOsMainOptions;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class AppVersionOptions extends CloudOsMainOptions {

    public static final String USAGE_NAME = "The name of the app. Required.";
    public static final String OPT_NAME = "-n";
    public static final String LONGOPT_NAME = "--name";
    @Option(name=OPT_NAME, aliases=LONGOPT_NAME, usage=USAGE_NAME, required=true)
    @Getter @Setter private String appName;

    public static final String USAGE_VERSION = "The version of the app. If omitted the latest version is assumed.";
    public static final String OPT_VERSION = "-r";
    public static final String LONGOPT_VERSION = "--version";
    @Option(name=OPT_VERSION, aliases=LONGOPT_VERSION, usage=USAGE_VERSION)
    @Getter @Setter private String appVersion;
    public boolean hasVersion () { return !empty(appVersion); }

    public String getVersionName () { return !hasVersion() ? "latest" : appVersion; }

}
