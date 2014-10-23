package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.util.concurrent.TimeUnit;

public class InstallAppOptions extends CloudOsMainOptions {

    public static final String USAGE_URL = "The url for the app. Required.";
    public static final String OPT_URL = "-u";
    public static final String LONGOPT_URL = "--url";
    @Option(name=OPT_URL, aliases=LONGOPT_URL, usage=USAGE_URL, required=true)
    @Getter @Setter private String url;
    
    public static final String USAGE_POLL_INTERVAL = "Number of seconds to sleep in between status checks. Default is 4.";
    public static final String OPT_POLL_INTERVAL = "-p";
    public static final String LONGOPT_POLL_INTERVAL = "--poll";
    @Option(name=OPT_POLL_INTERVAL, aliases=LONGOPT_POLL_INTERVAL, usage=USAGE_POLL_INTERVAL)
    @Getter @Setter private long pollInterval = TimeUnit.SECONDS.toMillis(4);

}
