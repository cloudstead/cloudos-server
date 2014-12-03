package cloudos.main.app;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class SyncAppRepositoryOptions {

    public static final String USAGE_EXPORTS = "The exports file for the app. Required.";
    public static final String OPT_EXPORTS = "-e";
    public static final String LONGOPT_EXPORTS = "--exports";
    @Option(name=OPT_EXPORTS, aliases=LONGOPT_EXPORTS, usage=USAGE_EXPORTS, required=true)
    @Getter @Setter private File exports;

}
