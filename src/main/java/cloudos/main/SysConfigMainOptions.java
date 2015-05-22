package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class SysConfigMainOptions extends CloudOsMainOptions {

    public boolean isEmpty () { return empty(appName) && empty(category) && empty(configOption) && empty(value); }

    public static final String USAGE_APP_NAME = "The name of the app to view/update settings for.";
    public static final String OPT_APP_NAME = "-n";
    public static final String LONGOPT_APP_NAME = "--name";
    @Option(name=OPT_APP_NAME, aliases=LONGOPT_APP_NAME, usage=USAGE_APP_NAME)
    @Getter @Setter private String appName;
    public boolean hasAppName () { return !empty(appName); }

    public static final String USAGE_CATEGORY = "The category of settings to view or update.";
    public static final String OPT_CATEGORY = "-c";
    public static final String LONGOPT_CATEGORY = "--category";
    @Option(name=OPT_CATEGORY, aliases=LONGOPT_CATEGORY, usage=USAGE_CATEGORY)
    @Getter @Setter private String category;
    public boolean hasCategory () { return !empty(category); }

    public static final String USAGE_CONFIG_OPTION = "The name of the option to view or update.";
    public static final String OPT_CONFIG_OPTION = "-o";
    public static final String LONGOPT_CONFIG_OPTION = "--option";
    @Option(name=OPT_CONFIG_OPTION, aliases=LONGOPT_CONFIG_OPTION, usage=USAGE_CONFIG_OPTION)
    @Getter @Setter private String configOption;
    public boolean hasConfigOption () { return !empty(configOption); }

    public static final String USAGE_VALUE = "The value of the option to update";
    public static final String OPT_VALUE = "-v";
    public static final String LONGOPT_VALUE = "--value";
    @Option(name=OPT_VALUE, aliases=LONGOPT_VALUE, usage=USAGE_VALUE)
    @Getter @Setter private String value;
    public boolean hasValue () { return !empty(value); }

}
