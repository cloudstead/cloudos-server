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
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.Tarball;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.security.ShaUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.empty;

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
            if (!f.renameTo(dest)) {
                error("{appDownload.error.renaming.contents}", new Exception("the bundle file/dir could not be moved: "+f.getAbsolutePath()+" -> "+dest.getAbsolutePath()));
                return cleanup(tarball, tempDir);
            }
        }

        // If assets are defined, ensure they will be accessible via https from local cloudstead
        boolean assetChanged = false;
        for (String asset : new String[] {"taskbarIcon", "smallIcon", "largeIcon"}) {
            assetChanged = validateAsset(manifest, asset, layout) || assetChanged;
        }
        if (assetChanged) {
            // rewrite manifest with new asset URLs
            FileUtil.toFileOrDie(layout.getManifest(), JsonUtil.toJson(manifest));
        }

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

    private boolean validateAsset(AppManifest manifest, String asset, AppLayout layout) {
        // does the manifest define this asset?
        File assetFile = null;
        String sha = null;
        AppMutableData assets = manifest.getAssets();
        if (assets != null) {
            final Object value = ReflectionUtil.get(assets, asset + "Url");
            final Object shaValue = ReflectionUtil.get(assets, asset + "UrlSha");
            if (value != null) {
                final String assetUrl = value.toString();
                final String ext = URIUtil.getFileExt(assetUrl);
                if (!isValidImageExtention(ext)) {
                    throw new IllegalStateException("Invalid file extension for asset (must be one of: "+ Arrays.toString(AppLayout.ASSET_IMAGE_EXTS) +"): "+assetUrl);
                }
                assetFile = new File(layout.getChefFilesDir(), asset + "." + ext);
                final File parent = assetFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Error creating directory: "+ parent.getAbsolutePath());

                try {
                    HttpUtil.url2file(assetUrl, assetFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Asset (" + asset + ") could not be loaded from: " + assetUrl, e);
                }
                if (!empty(shaValue)) sha = shaValue.toString();
            }
        }

        // no asset URL defined, check the app cookbook's "files/default" directory for a default asset
        if (assetFile == null) assetFile = layout.findDefaultAsset(asset);

        if (assetFile == null) return false;

        // calculate sha, validate if manifest specified one
        final String fileSha = ShaUtil.sha256_file(assetFile);
        if (!empty(sha) && !fileSha.equals(sha)) throw new IllegalStateException("Asset (" + assetFile.getAbsolutePath() + " had an invalid SHA sum");

        if (assets == null) {
            assets = new AppMutableData();
            manifest.setAssets(assets);
        }
        String base = configuration.getPublicUriBase();
        if (base.endsWith("/")) base = base.substring(0, base.length()-1);
        ReflectionUtil.set(assets, asset + "Url", base + configuration.getHttp().getBaseUri() +"/app_assets/"+manifest.getScrubbedName()+"/"+assetFile.getName());
        ReflectionUtil.set(assets, asset + "UrlSha", fileSha);
        return true;
    }

    private boolean isValidImageExtention(String ext) {
        for (String validExt : AppLayout.ASSET_IMAGE_EXTS) {
            if (ext.equals(validExt)) return true;
        }
        return false;
    }

    private TaskResult cleanup(File... files) {
        for (File f : files) if (f != null && !f.delete()) log.warn("Error deleting: "+f.getAbsolutePath());
        return null;
    }
}
