package cloudos.service;

import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.support.AppUninstallMode;
import cloudos.model.support.AppUninstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.CloudOsTaskResult;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.wizard.task.TaskBase;
import rooty.RootyMessage;
import rooty.toots.chef.ChefMessage;
import rooty.toots.chef.ChefOperation;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.io.FileUtil.abs;

@Accessors(chain=true) @Slf4j
public class AppUninstallTask extends TaskBase<CloudOsTaskResult> {

    private static final long UNINSTALL_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    @Getter @Setter private RootyService rootyService;
    @Getter @Setter private CloudOsAccount account;
    @Getter @Setter private AppUninstallRequest request;
    @Getter @Setter private AppDAO appDAO;
    @Getter @Setter private SessionDAO sessionDAO;
    @Getter @Setter private CloudOsConfiguration configuration;

    @Override public CloudOsTaskResult call() throws Exception {

        description("{appUninstall.uninstallingApp}", request.toString());

        // Find the app version to uninstall
        final String appName = request.getName();
        final AppLayout appLayout = configuration.getAppLayout(appName);
        final File appDir = appLayout.getAppDir();
        if (!appLayout.exists()) {
            error("{appUninstall.error.appNotFound}", "not a directory");
            return null;
        }

        // Send off a rooty task to remove app from run_list
        addEvent("{appUninstall.notifyingChef}");
        final AppManifest manifest = AppManifest.load(appLayout.getManifest());
        final ChefMessage chefMessage = new ChefMessage(ChefOperation.REMOVE);
        chefMessage.setCookbook(manifest.getName());

        final RootyMessage status;
        try {
            result.setRootyUuid(chefMessage.initUuid());
            status = rootyService.request(chefMessage, UNINSTALL_TIMEOUT);
        } catch (Exception e) {
            error("{appUninstall.error.notifyingChef}", e);
            return null;
        }

        if (status.isSuccess()) {
            addEvent("{appUninstall.removingFromAppRepository}");
            if (!FileUtils.deleteQuietly(appDir)) {
                error("{appUninstall.error.removingFromAppRepository}", "error deleting the app repository dir: "+abs(appDir));
            }

            if (request.getMode() == AppUninstallMode.delete_backups) {
                // Send off a rooty task to delete backups
                // todo: need to implement this
            }

            appDAO.resetApps();
            result.setSuccess(!appLayout.getVersionDir().exists());

        } else {
            result.setError(status.getLastError());
        }

        return result;
    }

}
