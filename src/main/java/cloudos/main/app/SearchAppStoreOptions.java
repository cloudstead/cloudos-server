package cloudos.main.app;

import cloudos.appstore.model.app.AppLevel;
import cloudos.appstore.model.support.AppStoreObjectType;
import cloudos.appstore.model.support.AppStoreQuery;
import cloudos.main.PagedCloudOsMainOptions;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

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

    public static final String USAGE_APPNAME = "Find only this app";
    public static final String OPT_APPNAME = "-N";
    public static final String LONGOPT_APPNAME = "--app-name";
    @Option(name=OPT_APPNAME, aliases=LONGOPT_APPNAME, usage=USAGE_APPNAME)
    @Getter @Setter private String appName = null;

    public AppStoreQuery getQuery () {
        return new AppStoreQuery(getPage())
                .setAppName(appName)
                .setLocale(locale)
                .setLevel(level)
                .setType(type);
    }
}
