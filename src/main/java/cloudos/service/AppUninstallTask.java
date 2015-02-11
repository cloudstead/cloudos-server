package cloudos.service;

import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.AppMetadata;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.support.AppUninstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.TaskBase;
import cloudos.service.task.TaskResult;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.wizard.model.SemanticVersion;
import rooty.RootyMessage;
import rooty.toots.chef.ChefMessage;
import rooty.toots.chef.ChefOperation;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Accessors(chain=true) @Slf4j
public class AppUninstallTask extends TaskBase {

    private static final long UNINSTALL_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    @Getter @Setter private RootyService rootyService;
    @Getter @Setter private CloudOsAccount account;
    @Getter @Setter private AppUninstallRequest request;
    @Getter @Setter private AppDAO appDAO;
    @Getter @Setter private SessionDAO sessionDAO;
    @Getter @Setter private CloudOsConfiguration configuration;

    @Override public TaskResult call() throws Exception {

        description("{appUninstall.uninstallingApp}", request.toString());

        // Find the app version to uninstall
        final String appName = request.getName();
        final AppLayout appLayout = configuration.getAppLayout(appName, request.getVersion());
        final File appDir = appLayout.getAppDir();
        if (!appLayout.exists()) {
            error("{appInstall.versionNotFound}", "not a directory");
            return null;
        }

        // If this is the active version, rewrite the app metadata to remove it
        addEvent("{appUninstall.updatingMetadata}");
        final AppMetadata appMetadata = appLayout.getAppMetadata();
        if (appMetadata != null && appMetadata.isVersion(request.getVersion())) {
            appMetadata.setActive_version(null);
            appMetadata.write(appDir);
        }

        // Send off a rooty task to remove app from run_list
        addEvent("{appUninstall.notifyingChef}");
        final AppManifest manifest = AppManifest.load(appLayout.getManifest());
        final ChefMessage chefMessage = new ChefMessage(ChefOperation.REMOVE);
        for (String recipe : manifest.getChefInstallRunlist()) {
            chefMessage.addRecipe(recipe.trim());
        }
        final RootyMessage status;
        try {
            result.setRootyUuid(chefMessage.initUuid());
            status = rootyService.request(chefMessage, UNINSTALL_TIMEOUT);
        } catch (Exception e) {
            error("{appUninstall.error.notifyingChef}", e);
            return null;
        }

        switch (request.getMode()) {
            case delete_backups:
                // Send off a rooty task to delete backups
                // todo: need to implement this

            case delete:
                // Send off a rooty task to delete files
                // todo: need to implement this
        }

        if (status.isSuccess()) {
            addEvent("{appUninstall.removingFromAppRepository}");
            FileUtils.deleteDirectory(appLayout.getVersionDir());

            final File[] versionDirs = appDir.listFiles(SemanticVersion.DIR_FILTER);
            if (versionDirs == null || versionDirs.length == 0) {
                log.info("No versions remaining, deleting appDir: "+appDir.getAbsolutePath());
                FileUtils.deleteDirectory(appDir);
            }

            appDAO.resetApps();
            result.setSuccess(!appLayout.getVersionDir().exists());

        } else {
            result.setError(status.getLastError());
        }

        return result;
    }

}
