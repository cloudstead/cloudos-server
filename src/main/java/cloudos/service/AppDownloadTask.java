package cloudos.service;

import cloudos.appstore.model.AppMutableData;
import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.dao.AppDAO;
import cloudos.model.support.AppDownloadRequest;
import cloudos.model.support.AppInstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.TaskBase;
import cloudos.service.task.TaskResult;
import cloudos.service.task.TaskService;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.Tarball;

import java.io.File;

import static org.cobbzilla.util.json.JsonUtil.toJson;

/**
 * Download an application and add it to your cloudstead's application library
 * This task:
 * <ol>
 * <li>downloads the app bundle</li>
 * <li>unrolls it to a temp dir</li>
 * <li>validates the manifest file</li>
 * <li>moves the app's data into a name+version directory in the cloudstead app library</li>
 * </ol>
 *
 * You cannot download over an existing name+version unless the overwrite flag is set on the AppDownloadRequest object
 */
@Accessors(chain=true) @Slf4j
public class AppDownloadTask extends TaskBase {

    @Getter @Setter private AppDownloadRequest request;
    @Getter @Setter private CloudOsConfiguration configuration;
    @Getter @Setter private TaskService taskService;
    @Getter @Setter private RootyService rootyService;
    @Getter @Setter private AppDAO appDAO;
    @Getter @Setter private CloudOsAccount account;

    @Override
    public TaskResult call() throws Exception {

        // initial description of task (we'll refine this when we know what is being installed)
        description("{appDownload.starting}", request.getUrl());

        // download the tarball to a tempfile
        addEvent("{appDownload.downloadingTarball}");
        final String suffix = request.getUrl().substring(request.getUrl().lastIndexOf('.'));
        File tarball = null;
        try {
            tarball = File.createTempFile("app-tarball-", suffix);
            HttpUtil.url2file(request.getDownloadUrl(), tarball);
        } catch (Exception e) {
            error("{appDownload.error.downloadingTarball", e);
            return cleanup(tarball);
        }

        // unroll the tarball to a temp dir
        addEvent("{appDownload.unpackingTarball}");
        final File tempDir;
        try {
            tempDir = Tarball.unroll(tarball);
        } catch (Exception e) {
            error("{appDownload.error.unpackingTarball}", e);
            return cleanup(tarball);
        }

        // validate the manifest
        addEvent("{appDownload.readingManifest}");
        final AppManifest manifest;
        try {
            manifest = AppManifest.load(tempDir);
        } catch (Exception e) {
            error("{appDownload.error.readingManifest}", e);
            return cleanup(tarball, tempDir);
        }

        // ensure app dir and app-version dirs exist
        final AppLayout layout = configuration.getAppLayout(manifest);
        final File appDir = layout.getAppDir();
        if (!appDir.exists() && !appDir.mkdirs()) {
            error("{appDownload.error.mkdir.appDir}", new Exception("error creating appDir: "+appDir.getAbsolutePath()));
            return cleanup(tarball, tempDir);
        }
        final File appVersionDir = layout.getAppVersionDir(manifest.getVersion());
        if (!appVersionDir.exists() && !appVersionDir.mkdirs()) {
            error("{appDownload.error.mkdir.appVersionDir}", new Exception("error creating appVersionDir: "+appVersionDir.getAbsolutePath()));
            return cleanup(tarball, tempDir);
        }

        // move tarball and unpacked-dir to permanent location
        final File destTarball = layout.getBundleFile();
        if (destTarball.exists() && !request.isOverwrite()) {
            error("{appDownload.error.alreadyExists}", new Exception("the app was already downloaded and 'overwrite' was not set"));
            return cleanup(tarball, tempDir);
        }
        if (!tarball.renameTo(destTarball)) {
            error("{appDownload.error.renaming.bundle}", new Exception("the bundle could not be renamed: "+tarball.getAbsolutePath()+" -> "+destTarball.getAbsolutePath()));
            return cleanup(tarball, tempDir);
        }

        // move unpacked dir into place
        final File[] appFiles = tempDir.listFiles();
        if (appFiles == null) {
            error("{appDownload.error.noFiles}", new Exception("the bundle did not contain any files: "+tarball.getAbsolutePath()));
            return cleanup(tarball, tempDir);
        }
        for (File f : appFiles) {
            final File dest = new File(appVersionDir, f.getName());
            if (dest.exists()) {
                if (!request.isOverwrite()) {
                    error("{appDownload.error.alreadyExists}", new Exception("the app already exists in your app-repository and 'overwrite' was not set"));
                    return cleanup(tarball, tempDir);
                } else {
                    FileUtils.deleteDirectory(dest);
                }
            }
            if (!f.renameTo(dest)) {
                error("{appDownload.error.renaming.contents}", new Exception("the bundle file/dir could not be moved: "+f.getAbsolutePath()+" -> "+dest.getAbsolutePath()));
                return cleanup(tarball, tempDir);
            }
        }

        // If assets are defined, ensure they will be accessible via https from local cloudstead
        AppMutableData.downloadAssetsAndUpdateManifest(manifest, layout, configuration.getAssetUrlBase());

        if (request.isAutoInstall() && !manifest.hasDatabags()) {
            // submit another job to do the install
            final AppInstallTask installTask = (AppInstallTask) new AppInstallTask()
                    .setAccount(account)
                    .setAppDAO(appDAO)
                    .setConfiguration(configuration)
                    .setRequest(new AppInstallRequest(manifest.getName(), manifest.getVersion(), false))
                    .setRootyService(rootyService)
                    .setResult(result);
            return installTask.call();
        }

        result.setReturnValue(toJson(manifest));
        result.setSuccess(true);
        return result;
    }

    private TaskResult cleanup(File... files) {
        for (File f : files) if (f != null && !f.delete()) log.warn("Error deleting: "+f.getAbsolutePath());
        return null;
    }
}
