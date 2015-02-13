package cloudos.main.app;

import cloudos.model.support.AppUninstallMode;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class AppUninstallMainOptions extends AppVersionOptions {

    public static final String USAGE_MODE = "uninstall: deactivates the app, leaves nothing else changed. delete: deactivates the app, removes it from app-repository and deletes all files on the cloudstead. delete_backups: does everything that 'delete' does, and additionally deletes all backup files (there is no going back, use with caution)";
    public static final String OPT_MODE = "-m";
    public static final String LONGOPT_MODE = "--mode";
    @Option(name=OPT_MODE, aliases=LONGOPT_MODE, usage=USAGE_MODE)
    @Getter @Setter private AppUninstallMode mode = AppUninstallMode.uninstall;

}
