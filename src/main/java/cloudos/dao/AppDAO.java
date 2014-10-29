package cloudos.dao;

import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppManifest;
import cloudos.cslib.storage.CsStorageEngine;
import cloudos.model.InstalledApp;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.InstalledAppLoader;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Repository @Slf4j
public class AppDAO extends UniquelyNamedEntityDAO<InstalledApp> {

    @Autowired private CloudOsConfiguration configuration;

    public List<InstalledApp> findActive() throws Exception { return findByField("active", true); }

    private final AtomicReference<Map<String, AppRuntimeDetails>> appDetails = new AtomicReference<>();

    public Map<String, AppRuntimeDetails> getAvailableAppDetails() throws Exception {
        // todo: periodically expire the list anyway (every 15 minutes?) and refetch freshly from S3
        if (this.appDetails.get() == null) {
            synchronized (this.appDetails) {
                if (this.appDetails.get() == null) {
                    // todo: package up default apps and treat them like any other appConfigName
                    final Map<String, AppRuntimeDetails> apps = new LinkedHashMap<>();
                    for (Map.Entry<String, AppRuntimeDetails> entry : InstalledAppLoader.APP_DETAILS_BY_NAME.entrySet()) {
                        apps.put(entry.getKey(), entry.getValue());
                    }
                    for (InstalledApp app : findActive()) {
                        apps.put(app.getManifest().getName(), app.getManifest().getInstalledAppDetails());
                    }
                    this.appDetails.set(apps);
                }
            }
        }
        return this.appDetails.get();
    }

    private final AtomicReference<Map<String, AppRuntime>> apps = new AtomicReference<>();

    public Map<String, AppRuntime> getAvailableRuntimes() throws Exception {
        if (this.apps.get() == null) {
            synchronized (this.apps) {
                if (this.apps.get() == null) {

                    final Map<String, AppRuntime> apps = new LinkedHashMap<>();
                    // todo: package builtin apps and treat them like any other
                    for (Map.Entry<String, AppRuntime> entry : InstalledAppLoader.APPS_BY_NAME.entrySet()) {
                        apps.put(entry.getKey(), entry.getValue());
                    }

                    for (InstalledApp app : findActive()) {
                        final AppManifest manifest = app.getManifest();
                        final String pluginJarPath = getPluginJarPath(manifest.getName(), manifest.getVersion());
                        final byte[] jarBytes = getStorageEngine().read(pluginJarPath);

                        final Class<? extends AppRuntime> appClass;
                        if (jarBytes == null) {
                            appClass = (Class<? extends AppRuntime>) getClass().getClassLoader().loadClass(manifest.getPlugin());
                        } else {
                            final File jar = File.createTempFile("AppDAO.getAvailableRuntimes", ".jar");
                            try (OutputStream out = new FileOutputStream(jar)) {
                                IOUtils.copy(new ByteArrayInputStream(jarBytes), out);
                            }
                            appClass = loadPluginClass(jar, manifest.getPlugin());
                        }
                        final AppRuntime appRuntime = appClass.newInstance();

                        appRuntime.setDetails(manifest.getInstalledAppDetails());
                        appRuntime.setAuthentication(manifest.getAuth());
                        apps.put(manifest.getName(), appRuntime);
                    }
                    this.apps.set(apps);
                }
            }
        }
        return this.apps.get();
    }

    public void resetAvailableApps() throws Exception {
        synchronized (appDetails) { appDetails.set(null); }
        synchronized (apps) { apps.set(null); }
    }

    public InstalledApp install(CloudOsAccount account, AppManifest manifest, File pluginJar, File tarball, int port) throws Exception {

        // write the tarball a blob
        writeTarball(manifest, tarball);

        // write plugin as a blob
        // plugin may not exist if they are using a built-in plugin (like ConfigurableAppRuntime)
        if (pluginJar.exists()) writePluginJar(manifest, pluginJar);

        InstalledApp app = findByName(manifest.getName());
        if (app == null) app = new InstalledApp();

        app.setPort(port);
        app.setManifest(manifest);
        app.setAccount(account.getName());
        app.setActive(true);
        app = createOrUpdate(app);

        resetAvailableApps();
        return app;
    }

    private String writeTarball(AppManifest manifest, File tarball) throws Exception {
        final String tarballPath = getTarballPath(manifest.getName(), manifest.getVersion());
        @Cleanup final InputStream in = new FileInputStream(tarball);
        @Cleanup final ByteArrayOutputStream out = new ByteArrayOutputStream((int) tarball.length());
        IOUtils.copy(in, out);
        getStorageEngine().write(tarballPath, out.toByteArray());
        return tarballPath;
    }

    private String writePluginJar(AppManifest manifest, File pluginJar) throws Exception {
        final String jarPath = getPluginJarPath(manifest.getName(), manifest.getVersion());
        @Cleanup final InputStream in = new FileInputStream(pluginJar);
        @Cleanup final ByteArrayOutputStream out = new ByteArrayOutputStream((int) pluginJar.length());
        IOUtils.copy(in, out);
        getStorageEngine().write(jarPath, out.toByteArray());
        return jarPath;
    }

    private String getTarballPath(String name, String version) throws Exception {
        return ShaUtil.sha256_filename(name+"_"+version+"_tarball"+configuration.getCloudConfig().getDataKey());
    }

    private String getPluginJarPath(String name, String version) throws Exception {
        return ShaUtil.sha256_filename(name+"_"+version+"_pluginJar"+configuration.getCloudConfig().getDataKey());
    }

    public AppRuntime findAppRuntime(String appName) throws Exception { return getAvailableRuntimes().get(appName); }

    private CsStorageEngine getStorageEngine() { return configuration.getCloudConfig().getStorageEngine(); }

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
