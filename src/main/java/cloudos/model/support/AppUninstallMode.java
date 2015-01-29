package cloudos.model.support;

/**
 * uninstall: deactivates the app, leaves nothing else changed.
 * delete: deactivates the app, removes it from app-repository and deletes all files on the cloudstead.
 * delete_backups: does everything that 'delete' does, and additionally deletes all backup files (there is no going back, use with caution)";
 */
public enum AppUninstallMode {

    uninstall, delete, delete_backups

}
