package cloudos.main.app;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.string.StringUtil;
import org.kohsuke.args4j.Option;

public class AppConfigOptions extends AppVersionOptions {

    public static final String USAGE_CONFIG = "The JSON AppConfiguration to set for the app. " +
            "If absent, the current configuration will be displayed. " +
            "If this starts with / or ./ then the JSON will be read from the path provided";
    public static final String OPT_CONFIG = "-c";
    public static final String LONGOPT_CONFIG = "--config";
    @Option(name=OPT_CONFIG, aliases=LONGOPT_CONFIG, usage=USAGE_CONFIG)
    @Getter @Setter private String config;

    public boolean hasConfig () { return !StringUtil.empty(config); }
    public boolean isFileConfig () { return config != null && (config.startsWith("/") || config.startsWith("./")); }

    public String getConfigJson () { return isFileConfig() ? FileUtil.toStringOrDie(config) : config; }

}
