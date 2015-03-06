package cloudos.dao;

import cloudos.appstore.model.AppMutableData;
import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.AppMetadata;
import cloudos.model.Account;
import cloudos.appstore.model.app.config.AppConfiguration;
import cloudos.model.app.AppRepositoryState;
import cloudos.model.app.CloudOsApp;
import cloudos.model.support.AppDownloadRequest;
import cloudos.model.support.AppInstallRequest;
import cloudos.model.support.AppUninstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.AppDownloadTask;
import cloudos.service.AppInstallTask;
import cloudos.service.AppUninstallTask;
import cloudos.service.RootyService;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.cobbzilla.util.io.DirFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.model.SemanticVersion;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.json.JsonUtil.fromJson;

@Repository @Slf4j
public class AppDAO {

    @Autowired private TaskService taskService;
    @Autowired private RootyService rootyService;
    @Autowired private CloudOsConfiguration configuration;

    public AppRepositoryState getAppRepositoryState() {
        final AppRepositoryState state = new AppRepositoryState();
        for (File appDir : configuration.getAppRepository().listFiles(DirFilter.instance)) {

            final String appName = appDir.getName();
            final AppLayout layout = configuration.getAppLayout(appName);
            final File activeVersion = layout.getAppActiveVersionDir();

            for (File versionDir : appDir.listFiles(SemanticVersion.DIR_FILTER)) {
                final String appVersion = versionDir.getName();
                final File manifestFile = configuration.getAppLayout(appName, appVersion).getManifest();
                if (manifestFile.exists()) {
                    final AppManifest manifest = AppManifest.load(manifestFile);
                    if (versionDir.equals(activeVersion)) {
                        state.addApp(manifest, true);
                    } else {
                        state.addApp(manifest, false);
                    }
                }
            }
        }
        return state;
    }

    /**
     * Download an app to the cloudstead app library
     *
     * @param admin   The account making the request (must be admin)
     * @param request The download request
     * @return a TaskId that the caller can use to check on the status of the request
     */
    public TaskId download(Account admin, AppDownloadRequest request) {
        if (!admin.isAdmin()) throw new IllegalArgumentException("must be admin");

        // start background job
        final AppDownloadTask task = new AppDownloadTask()
                .setAccount(admin)
                .setAppDAO(this)
                .setRequest(request)
                .setConfiguration(configuration)
                .setTaskService(taskService)
                .setRootyService(rootyService);

        return taskService.execute(task);
    }

    /**
     * Get configuration settings for a particular app version, using the default locale for label translations
     *
     * @param app     The name of the app
     * @param version The version of the app
     * @return the app configuration
     */
    public AppConfiguration getConfiguration(String app, String version) {
        final AppLayout layout = configuration.getAppLayout(app, version);
        return AppConfiguration.fromLayout(layout, null);
    }

    /**
     * Get configuration settings for a particular app version.
     *
     * @param app     The name of the app
     * @param version The version of the app
     * @param locale  The locale of the user (configuration may include locale-specific translations for field labels/etc)
     * @return the app configuration
     */
    public AppConfiguration getConfiguration(String app, String version, String locale) {
        final AppLayout layout = configuration.getAppLayout(app, version);
        return AppConfiguration.fromLayout(layout, locale);
    }

    /**
     * Set configuration options for a particular app version
     *
     * @param app     The name of the app
     * @param version The version of the app
     * @param config  The new configuration. Any fields that are missing will not be written (existing settings will be preserved)
     */
    public void setConfiguration(String app, String version, AppConfiguration config) {
        final AppLayout layout = configuration.getAppLayout(app, version);

        if (!layout.exists()) throw new IllegalArgumentException("App/version does not exist: " + app + "/" + version);

        final AppManifest manifest = AppManifest.load(layout.getManifest());
        final File databagsDir = layout.getDatabagsDir();

        AppConfiguration.setAppConfiguration(manifest, databagsDir, config);
    }

    public TaskId install(Account admin, String app, String version, boolean force) {
        // start background job
        final AppInstallTask task = new AppInstallTask()
                .setAccount(admin)
                .setAppDAO(this)
                .setRequest(new AppInstallRequest(app, version, force))
                .setRootyService(rootyService)
                .setConfiguration(configuration);

        return taskService.execute(task);
    }

    public TaskId uninstall(Account admin, String app, String version, AppUninstallRequest request) {
        // start background job
        final AppUninstallTask task = new AppUninstallTask()
                .setAccount(admin)
                .setAppDAO(this)
                .setRequest(request)
                .setRootyService(rootyService)
                .setConfiguration(configuration);

        return taskService.execute(task);
    }

    public CloudOsApp findInstalledByName(String name) {
        return loadApp(configuration.getAppLayout(name).getAppDir());
    }

    public CloudOsApp findLatestVersionByName(String name) {
        final AppLayout appLayout = configuration.getAppLayout(name);
        final File latestVersionDir = appLayout.getLatestVersionDir();
        return latestVersionDir != null && latestVersionDir.exists() ? loadApp(appLayout.getAppDir(), latestVersionDir, null, false) : null;
    }

    public List<CloudOsApp> findActive() {
        final List<CloudOsApp> apps = new ArrayList<>();
        final File appRepository = configuration.getAppRepository();
        final File[] appDirs = FileUtil.list(appRepository);
        for (File appDir : appDirs) {
            final AppMetadata metadata = AppMetadata.fromJson(appDir);
            if (metadata.isActive()) {
                final CloudOsApp app = loadApp(appDir, metadata, false);
                if (app != null) apps.add(app);
            }

        }
        return apps;
    }

    private CloudOsApp loadApp(File appDir) {
        return loadApp(appDir, AppMetadata.fromJson(appDir));
    }

    private CloudOsApp loadApp(File appDir, AppMetadata metadata) {
        return loadApp(appDir, metadata, false);
    }

    private CloudOsApp loadApp(File appDir, AppMetadata metadata, boolean loadDataBags) {

        if (!appDir.exists() || !appDir.isDirectory()) return null;

        final String appName = appDir.getName();
        final AppLayout layout = configuration.getAppLayout(appName);

        final File versionDir = layout.getAppActiveVersionDir();
        if (versionDir == null) {
            log.warn("App " + appName + " downloaded but no version is active");
            return null;
        }

        return loadApp(appDir, versionDir, metadata, loadDataBags);
    }

    protected CloudOsApp loadApp(File appDir, File versionDir, AppMetadata metadata, boolean loadDataBags) {

        final String appName = appDir.getName();
        final AppLayout layout = configuration.getAppLayout(appName);

        final CloudOsApp app = new CloudOsApp()
                .setName(appDir.getName())
                .setAppRepository(configuration.getAppRepository())
                .setMetadata(metadata);

        final File databagsDir = layout.getDatabagsDir();

        try {
            app.setManifest(AppManifest.load(versionDir));

            if (loadDataBags && databagsDir.exists()) {
                // list all data bag groups (under data_bags dir)
                final File[] databagDirs = databagsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
                if (databagDirs != null) {
                    for (File databagDir : databagDirs) {
                        // list all data bags for one group (under data_bags/group-name/*.json)
                        final File[] databagFiles = databagDir.listFiles(JsonUtil.JSON_FILES);
                        if (databagFiles != null) {
                            for (File databagFile : databagFiles) {
                                final JsonNode databag = fromJson(FileUtil.toString(databagFile), JsonNode.class);
                                app.addDatabag(databagName(databagFile), databag);
                            }
                        }
                    }
                }
            }
            return app;

        } catch (Exception e) {
            log.error("loadApp(" + appDir + ", " + metadata + ") error: " + e, e);
            return null;
        }
    }

    private String databagName(File databagFile) {
        final String name = databagFile.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1)
            throw new IllegalArgumentException("Invalid databag file: " + name);
        return name.substring(0, lastDot);
    }

    private final AtomicReference<Map<String, AppRuntimeDetails>> appDetails = new AtomicReference<>();

    public Map<String, AppRuntimeDetails> getAvailableAppDetails() {
        if (this.appDetails.get() == null) {
            synchronized (this.appDetails) {
                if (this.appDetails.get() == null) {
                    final Map<String, AppRuntimeDetails> detailsMap = new HashMap<>();
                    for (Map.Entry<String, AppRuntime> entry : getAvailableRuntimes().entrySet()) {
                        detailsMap.put(entry.getKey(), entry.getValue().getDetails());
                    }
                    this.appDetails.set(detailsMap);
                }
            }
        }
        return this.appDetails.get();
    }

    private final AtomicReference<Map<String, AppRuntime>> apps = new AtomicReference<>();

    public Map<String, AppRuntime> getAvailableRuntimes() {
        if (this.apps.get() == null) {
            synchronized (this.apps) {
                if (this.apps.get() == null) {
                    try {
                        this.apps.set(initAvailableRuntimes());
                    } catch (Exception e) {
                        final String msg = "getAvailableRuntimes: error intializing: " + e;
                        log.error(msg, e);
                        throw new IllegalStateException(msg, e);
                    }
                }
            }
        }
        return this.apps.get();
    }

    private Map<String, AppRuntime> initAvailableRuntimes() throws Exception {

        final Map<String, CloudOsApp> appCache = new HashMap<>();
        final Map<String, AppRuntime> runtimes = new HashMap<>();

        // load runtimes
        for (CloudOsApp app : findActive()) {
            final AppManifest manifest = app.getManifest();
            final AppLayout layout = configuration.getAppLayout(manifest.getName());
            final File versionDir = layout.getAppActiveVersionDir(manifest);
            final File pluginJar = layout.getPluginJar();

            final Class<? extends AppRuntime> appClass;
            if (pluginJar.exists()) {
                appClass = loadPluginClass(pluginJar, manifest.getPlugin());
            } else {
                appClass = (Class<? extends AppRuntime>) getClass().getClassLoader().loadClass(manifest.getPlugin());
            }

            final AppRuntime appRuntime = appClass.newInstance();
            AppMutableData.downloadAssetsAndUpdateManifest(manifest, layout, configuration.getAssetUrlBase());
            appRuntime.setDetails(manifest.getInstalledAppDetails());
            appRuntime.setAuthentication(manifest.getAuth());

            appCache.put(manifest.getName(), app);
            runtimes.put(manifest.getName(), appRuntime);
        }

        // for apps that have a parent, merge parent runtime into child
        final Map<String, AppRuntime> apps = new LinkedHashMap<>();
        for (CloudOsApp app : appCache.values()) {
            final AppManifest manifest = app.getManifest();
            final AppRuntime appRuntime = runtimes.get(manifest.getName());
            if (manifest.hasParent()) {
                final AppRuntime parentRuntime = runtimes.get(manifest.getParent());
                mergeParent(appRuntime, parentRuntime);
            }
            apps.put(manifest.getName(), appRuntime);
        }

        return apps;
    }

    private void mergeParent(AppRuntime appRuntime, AppRuntime parentRuntime) {
        appRuntime.getDetails().mergeParent(parentRuntime.getDetails());
        appRuntime.setAuthentication(parentRuntime.getAuthentication());
    }

    public AppRuntime findAppRuntime(String appName) {
        return getAvailableRuntimes().get(appName);
    }

    public Class<? extends AppRuntime> loadPluginClass(File pluginJar, String pluginClassName) throws SimpleViolationException {
        final Class<? extends AppRuntime> pluginClass;
        try {
            final ClassLoader loader = new URLClassLoader(new URL[]{pluginJar.toURI().toURL()}, getClass().getClassLoader());
            pluginClass = (Class<AppRuntime>) loader.loadClass(pluginClassName);

        } catch (Exception e) {
            throw new SimpleViolationException("{error.installApp.pluginClass.errorLoading}", "The sso class specified in the cloudos-manifest.json file could not be loaded", pluginClassName);
        }

        if (!AppRuntime.class.isAssignableFrom(pluginClass)) {
            throw new SimpleViolationException("{error.installApp.pluginClass.doesNotImplementInstalledApp}", "The sso class specified in the cloudos-manifest.json file does not implement the AppRuntime interface", pluginClass.getName());
        }
        return pluginClass;
    }

    public void resetApps() {
        synchronized (this.apps) {
            synchronized (this.appDetails) {
                this.apps.set(null);
                this.appDetails.set(null);
            }
        }
    }

}