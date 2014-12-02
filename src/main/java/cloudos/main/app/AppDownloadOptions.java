package cloudos.main.app;

import cloudos.main.CloudOsMainOptions;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class AppDownloadOptions extends CloudOsMainOptions {

    public static final String USAGE_URL = "The url for the app. Required.";
    public static final String OPT_URL = "-u";
    public static final String LONGOPT_URL = "--url";
    @Option(name=OPT_URL, aliases=LONGOPT_URL, usage=USAGE_URL, required=true)
    @Getter @Setter private String url;

    public static final String USAGE_TOKEN = "The token for the app. Required for paid apps.";
    public static final String OPT_TOKEN = "-t";
    public static final String LONGOPT_TOKEN = "--token";
    @Option(name=OPT_TOKEN, aliases=LONGOPT_TOKEN, usage=USAGE_TOKEN, required=true)
    @Getter @Setter private String token;

    public static final String USAGE_INSTALL = "Automatically install the app if it does not require any additional configuration.";
    public static final String OPT_INSTALL = "-i";
    public static final String LONGOPT_INSTALL = "--install";
    @Option(name=OPT_INSTALL, aliases=LONGOPT_INSTALL, usage=USAGE_INSTALL)
    @Getter @Setter private boolean install = false;

    public static final String USAGE_OVERWRITE = "Overwrite the app in the local app-repository even if the same version is already present.";
    public static final String OPT_OVERWRITE = "-o";
    public static final String LONGOPT_OVERWRITE = "--overwrite";
    @Option(name=OPT_OVERWRITE, aliases=LONGOPT_OVERWRITE, usage=USAGE_OVERWRITE)
    @Getter @Setter private boolean overwrite = false;

}
