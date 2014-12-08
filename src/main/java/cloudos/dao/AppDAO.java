package cloudos.dao;

import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.app.AppDatabagDef;
import cloudos.appstore.model.app.AppManifest;
import cloudos.model.Account;
import cloudos.model.app.*;
import cloudos.model.support.AppDownloadRequest;
import cloudos.model.support.AppInstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.AppDownloadTask;
import cloudos.service.AppInstallTask;
import cloudos.service.RootyService;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonEdit;
import org.cobbzilla.util.json.JsonEditOperation;
import org.cobbzilla.util.json.JsonEditOperationType;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.json.JsonUtil.FULL_MAPPER;
import static org.cobbzilla.util.json.JsonUtil.fromJson;

@Repository @Slf4j
public class AppDAO {

    @Autowired private TaskService taskService;
    @Autowired private RootyService rootyService;
    @Autowired private CloudOsConfiguration configuration;

    /**
     * Download an app to the cloudstead app library
     * @param admin The account making the request (must be admin)
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
     * Get configuration settings for a particular app version.
     * @param app The name of the app
     * @param version The version of the app
     * @return the app configuration
     */
    public AppConfiguration getConfiguration (String app, String version) {

        final CloudOsAppLayout layout = configuration.getAppLayout();

        final File appVersionDir = layout.getAppVersionDir(app, version);
        if (!appVersionDir.exists()) throw new IllegalArgumentException("App does not exist: "+app+"/"+version);

        final AppManifest manifest = AppManifest.load(layout.getManifest(appVersionDir));
        final AppConfiguration config = new AppConfiguration();
        if (manifest.hasDatabags()) {
            for (AppDatabagDef databag : manifest.getDatabags()) {

                final String databagName = databag.getName();
                final File databagFile = layout.getDatabagFile(appVersionDir, databagName);
                JsonNode node = null;
                if (databagFile.exists()) {
                    node = JsonUtil.fromJsonOrDie(FileUtil.toStringOrDie(databagFile), JsonNode.class);
                }

                final AppConfigurationCategory category = new AppConfigurationCategory(databagName);
                config.add(category);
                for (String item : databag.getItems()) {
                    category.add(item);
                    if (node != null) {
                        try {
                            final JsonNode itemNode = JsonUtil.findNode(node, item);
                            if (itemNode != null) category.set(item, itemNode.asText());

                        } catch (IOException e) {
                            log.warn("Error loading databag item ("+databagName+"/"+item+"): "+e);
                        }
                    }
                }
            }
        }
        return config;
    }

    /**
     * Set configuration options for a particular app version
     * @param app The name of the app
     * @param version The version of the app
     * @param config The new configuration. Any fields that are missing will not be written (existing settings will be preserved)
     */
    public void setConfiguration (String app, String version, AppConfiguration config) {
        final CloudOsAppLayout layout = configuration.getAppLayout();

        final File appVersionDir = layout.getAppVersionDir(app, version);
        if (!appVersionDir.exists()) throw new IllegalArgumentException("App does not exist: " + app + "/" + version);

        final AppManifest manifest = AppManifest.load(layout.getManifest(appVersionDir));
        if (manifest.hasDatabags()) {
            for (AppDatabagDef databag : manifest.getDatabags()) {

                final String databagName = databag.getName();

                final List<JsonEditOperation> operations = new ArrayList<>();
                operations.add(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("name")
                        .setJson("\"" + databagName + "\""));

                // Did the caller provide config for this category?
                final AppConfigurationCategory category = config.getCategory(databagName);
                if (category == null) {
                    log.warn("No configuration provided for category ("+databagName+"), skipping");
                    continue;
                }

                // Does this category exist as a databag? If not create a new JsonNode to represent it
                final File databagFile = layout.getDatabagFile(appVersionDir, databagName);
                final JsonNode node;
                if (databagFile.exists()) {
                    node = layout.getDatabag(appVersionDir, databagName);
                } else {
                    node = new ObjectNode(FULL_MAPPER.getNodeFactory());
                }

                // Update config settings via JSON
                for (String item : databag.getItems()) {
                    // Did the caller provide a value for this config item?
                    final String value = category.getValues().get(item);
                    if (value != null) {
                        // If the setting already exists in the data bag, determine the type from there
                        final JsonEditOperation op = new JsonEditOperation()
                                .setType(JsonEditOperationType.write)
                                .setPath(item);

                        try {
                            final JsonNode existing = JsonUtil.findNode(node, item);
                            final String json;
                            if (existing != null) {
                                json = JsonUtil.toJson(JsonUtil.getValueNode(existing, item, value));
                            } else {
                                json = "\"" + value + "\""; // assume String
                            }
                            op.setJson(json);
                            operations.add(op);

                        } catch (Exception e) {
                            throw new IllegalStateException("Error preparing to write "+databagName+"/"+item+": "+e);
                        }
                    }
                }

                final String updatedJson;
                try {
                    updatedJson = new JsonEdit().setJsonData(node).setOperations(operations).edit();
                    FileUtil.toFile(databagFile, updatedJson);

                } catch (Exception e) {
                    throw new IllegalStateException("Error generating updated json: "+e);
                }

            }
        }
    }

    public TaskId install(Account admin, String app, String version) {
        // start background job
        final AppInstallTask task = new AppInstallTask()
                .setAccount(admin)
                .setAppDAO(this)
                .setRequest(new AppInstallRequest(app, version))
                .setRootyService(rootyService)
                .setConfiguration(configuration);

        return taskService.execute(task);
    }

    public CloudOsApp findByName(String name) {
        return loadApp(configuration.getAppLayout().getAppDir(name));
    }

    public List<CloudOsApp> findActive() {
        final List<CloudOsApp> apps = new ArrayList<>();
        final File appRepository = configuration.getAppRepository();
        final File[] appDirs = appRepository.listFiles();
        if (appDirs == null) throw new IllegalStateException("Error listing app repository");
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

    private CloudOsApp loadApp(File appDir, AppMetadata metadata) { return loadApp(appDir, metadata, false); }

    private CloudOsApp loadApp(File appDir, AppMetadata metadata, boolean loadDataBags) {

        if (!appDir.exists() || !appDir.isDirectory()) return null;

        final CloudOsApp app = new CloudOsApp()
                .setName(appDir.getName())
                .setAppRepository(configuration.getAppRepository())
                .setMetadata(metadata);

        final CloudOsAppLayout layout = configuration.getAppLayout();
        final File versionDir = layout.getAppActiveVersionDir(app.getName());
        final File databagsDir = layout.getDatabagsDir(versionDir);
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
            log.error("loadApp("+appDir+", "+metadata+") error: "+e, e);
            return null;
        }
    }

    private String databagName(File databagFile) {
        final String name = databagFile.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length()-1) throw new IllegalArgumentException("Invalid databag file: "+name);
        return name.substring(0, lastDot);
    }

    private final AtomicReference<Map<String, AppRuntimeDetails>> appDetails = new AtomicReference<>();

    public Map<String, AppRuntimeDetails> getAvailableAppDetails() {
        if (this.appDetails.get() == null) {
            synchronized (this.appDetails) {
                if (this.appDetails.get() == null) {
                    final Map<String, AppRuntimeDetails> apps = new LinkedHashMap<>();
                    for (CloudOsApp app : findActive()) {
                        apps.put(app.getManifest().getName(), app.getManifest().getInstalledAppDetails());
                    }
                    this.appDetails.set(apps);
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
        final Map<String, AppRuntime> apps = new LinkedHashMap<>();
        final CloudOsAppLayout layout = configuration.getAppLayout();
        for (CloudOsApp app : findActive()) {
            final AppManifest manifest = app.getManifest();
            final File versionDir = layout.getAppActiveVersionDir(manifest);
            final File pluginJar = layout.getPluginJar(versionDir);

            final Class<? extends AppRuntime> appClass;
            if (pluginJar.exists()) {
                appClass = loadPluginClass(pluginJar, manifest.getPlugin());
            } else {
                appClass = (Class<? extends AppRuntime>) getClass().getClassLoader().loadClass(manifest.getPlugin());
            }
            final AppRuntime appRuntime = appClass.newInstance();

            appRuntime.setDetails(manifest.getInstalledAppDetails());
            appRuntime.setAuthentication(manifest.getAuth());
            apps.put(manifest.getName(), appRuntime);
        }
        return apps;
    }

    public AppRuntime findAppRuntime(String appName) { return getAvailableRuntimes().get(appName); }

    public Class<? extends AppRuntime> loadPluginClass(File pluginJar, String pluginClassName) throws SimpleViolationException {
        final Class<? extends AppRuntime> pluginClass;
        try {
            final ClassLoader loader = new URLClassLoader(new URL[]{pluginJar.toURI().toURL()}, getClass().getClassLoader());
            pluginClass = (Class<AppRuntime>) loader.loadClass(pluginClassName);

        } catch (Exception e)  {
            throw new SimpleViolationException("{error.installApp.pluginClass.errorLoading}", "The sso class specified in the cloudos-manifest.json file could not be loaded", pluginClassName);
        }

        if (!AppRuntime.class.isAssignableFrom(pluginClass)) {
            throw new SimpleViolationException("{error.installApp.pluginClass.doesNotImplementInstalledApp}", "The sso class specified in the cloudos-manifest.json file does not implement the AppRuntime interface", pluginClass.getName());
        }
        return pluginClass;
    }

}
