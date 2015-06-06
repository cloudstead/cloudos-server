package cloudos.main.app;

import cloudos.appstore.model.app.AppLevel;
import cloudos.appstore.model.support.AppStoreObjectType;
import cloudos.appstore.model.support.AppStoreQuery;
import cloudos.main.PagedCloudOsMainOptions;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class SearchAppStoreOptions extends PagedCloudOsMainOptions {

    public static final String USAGE_LEVEL = "Only show apps at this level. Default is 'app'";
    public static final String OPT_LEVEL = "-l";
    public static final String LONGOPT_LEVEL = "--level";
    @Option(name=OPT_LEVEL, aliases=LONGOPT_LEVEL, usage=USAGE_LEVEL)
    @Getter @Setter private AppLevel level = AppLevel.app;

    public static final String USAGE_TYPE = "Only search fields from this portion of an app. If not set, all fields will be searched.";
    public static final String OPT_TYPE = "-T";
    public static final String LONGOPT_TYPE = "--type";
    @Option(name=OPT_TYPE, aliases=LONGOPT_TYPE, usage=USAGE_TYPE)
    @Getter @Setter private AppStoreObjectType type;

    public static final String USAGE_LOCALE = "Use this locale";
    public static final String OPT_LOCALE = "-L";
    public static final String LONGOPT_LOCALE = "--locale";
    @Option(name=OPT_LOCALE, aliases=LONGOPT_LOCALE, usage=USAGE_LOCALE)
    @Getter @Setter private String locale = null;

    public static final String USAGE_PUBLISHER = "Find apps with this publisher";
    public static final String OPT_PUBLISHER = "-P";
    public static final String LONGOPT_PUBLISHER = "--publisher";
    @Option(name=OPT_PUBLISHER, aliases=LONGOPT_PUBLISHER, usage=USAGE_PUBLISHER)
    @Getter @Setter private String publisher = null;
    public boolean hasPublisher () { return !empty(publisher); }

    public static final String USAGE_APPNAME = "Find only this app";
    public static final String OPT_APPNAME = "-n";
    public static final String LONGOPT_APPNAME = "--app";
    @Option(name=OPT_APPNAME, aliases=LONGOPT_APPNAME, usage=USAGE_APPNAME)
    @Getter @Setter private String app = null;
    public boolean hasApp () { return !empty(app); }

    public static final String USAGE_VERSION = "Find only this app version";
    public static final String OPT_VERSION = "-r";
    public static final String LONGOPT_VERSION = "--version";
    @Option(name=OPT_VERSION, aliases=LONGOPT_VERSION, usage=USAGE_VERSION)
    @Getter @Setter private String version = null;
    public boolean hasVersion () { return !empty(version); }

    public AppStoreQuery getQuery () {
        return new AppStoreQuery(getPage())
                .setPublisher(publisher)
                .setApp(app)
                .setVersion(version)
                .setLocale(locale)
                .setLevel(level)
                .setType(type);
    }
}
