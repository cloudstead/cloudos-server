package cloudos.main.app;

import cloudos.appstore.model.app.AppManifest;
import cloudos.model.app.AppMetadata;
import cloudos.model.app.CloudOsAppLayout;
import cloudos.server.CloudOsConfiguration;
import cloudos.server.CloudOsServer;
import com.fasterxml.jackson.core.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.RestServerConfigurationFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import rooty.toots.chef.ChefHandler;
import rooty.toots.chef.ChefMessage;
import rooty.toots.chef.ChefSolo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class SyncAppRepositoryMain {

    @Getter @Setter private SyncAppRepositoryOptions options = new SyncAppRepositoryOptions();
    private final CmdLineParser parser = new CmdLineParser(options);

    @Getter private String[] args;
    public void setArgs(String[] args) throws CmdLineException {
        this.args = args;
        parser.parseArgument(args);
    }

    public static void main (String[] args) {
        if (!CommandShell.whoami().equals("root")) throw new IllegalStateException("Must run as root");

        try {
            final SyncAppRepositoryMain m = new SyncAppRepositoryMain();
            m.setArgs(args);

            final CloudOsConfiguration configuration = loadConfiguration(m.getOptions());

            // force app-repository to reference same dir as exports file
            final File cloudosUserHome = m.getOptions().getExports().getParentFile();
            configuration.setAppRepository(new File(cloudosUserHome, CloudOsConfiguration.APP_REPOSITORY));

            m.synchronize(cloudosUserHome.getName(), configuration);

        } catch (Exception e) {
            log.error("Unexpected error: "+e, e);
        }
    }

    private static CloudOsConfiguration loadConfiguration (SyncAppRepositoryOptions options) throws IOException {

        // load environment variable
        final Map<String, String> env = CommandShell.loadShellExports(options.getExports());

        // load configuration sources the same way the server does
        final List<? extends ConfigurationSource> configurations = CloudOsServer.getConfigurationSources();

        // build the configuration
        final RestServerConfigurationFactory<CloudOsConfiguration> factory = new RestServerConfigurationFactory<>(CloudOsConfiguration.class);
        return factory.build(configurations, env);
    }

    /**
     * Read authoritative solo.json, populate app-repository if we are missing anything that is already installed
     */
    public void synchronize(String cloudosUser, CloudOsConfiguration configuration) {

        final File chefDir = new File(new ChefHandler().getChefDir());
        final File soloJsonFile = new File(chefDir, "solo.json");
        if (!soloJsonFile.exists()) throw new IllegalStateException("No solo.json file found: "+soloJsonFile.getAbsolutePath());

        JsonUtil.FULL_MAPPER.getFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);
        final ChefSolo chefSolo;
        try {
            chefSolo = JsonUtil.fromJson(FileUtil.toString(soloJsonFile), ChefSolo.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error reading "+soloJsonFile.getAbsolutePath()+": "+e);
        }

        for (String recipe : chefSolo.getRun_list()) {
            // look for 'default' recipes
            try {
                if (!recipe.contains("::")) {
                    final String app = ChefMessage.getCookbook(recipe);
                    if (app != null) {
                        // Look for cloudos-manifest.json in databag dir for app
                        final File manifestFile = new File(chefDir.getAbsolutePath()+"/data_bags/"+app+"/"+ AppManifest.CLOUDOS_MANIFEST_JSON);
                        if (manifestFile.exists()) {
                            // Load the manifest
                            final AppManifest manifest = AppManifest.load(manifestFile);

                            // Does the app-repository contain this app+version?
                            final CloudOsAppLayout layout = configuration.getAppLayout();
                            final File appVersionDir = layout.getAppVersionDir(manifest.getName(), manifest.getVersion());
                            if (!appVersionDir.exists()) {
                                registerApp(configuration, manifestFile, manifest, chefDir);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.warn("Error inspecting recipe "+recipe+" while looking for apps: "+e);

            } finally {
                // ensure repository keeps proper ownership and permissions
                try {
                    CommandShell.chown(cloudosUser+"."+configuration.getRootyGroup(), configuration.getAppRepository(), true);
                    CommandShell.chmod(configuration.getAppRepository(), "750", true);
                } catch (Exception e) {
                    throw new IllegalStateException("Error setting ownership/permissions on "+configuration.getAppRepository().getAbsolutePath()+": "+e, e);
                }
            }
        }
    }

    /**
     * Called when an app is found in the chef-solo repo, but not in the cloudos app-repository.
     * @param manifestFile The manifest file in the main chef-solo directory
     * @param manifest The manifest found in the chef-solo databags dir for this app
     * @param chefDir The main chef solo directory
     */
    private void registerApp(CloudOsConfiguration configuration, File manifestFile, AppManifest manifest, File chefDir) throws Exception {

        final String app = manifest.getScrubbedName();
        final String version = manifest.getVersion();

        final CloudOsAppLayout layout = configuration.getAppLayout();
        final File appDir = layout.getAppDir(app);
        final File appVersionDir = layout.getAppVersionDir(manifest.getName(), version);

        final String chefPath = chefDir.getAbsolutePath();
        final String repoChefPath = layout.getChefDir(appVersionDir).getAbsolutePath();

        // Copy: [chefDir]/cookbooks/app/* -> [appVersionDir]/chef/cookbooks/app/
        FileUtils.copyDirectory(new File(chefPath + "/cookbooks/" + app),
                new File(repoChefPath + "/cookbooks/" + app));

        // Copy: [chefDir]/data_bags/app/* -> [appVersionDir]/chef/data_bags/app/
        FileUtils.copyDirectory(new File(chefPath + "/data_bags/" + app),
                new File(repoChefPath + "/data_bags/" + app));

        // Copy: [chefDir]/data_bags/app/cloudos-manifest.json -> [appVersionDir]/
        FileUtils.copyFile(manifestFile, new File(appVersionDir, AppManifest.CLOUDOS_MANIFEST_JSON));

        // Update: [appDir]/metadata.json (set active version if needed)
        AppMetadata metadata = AppMetadata.fromJson(appDir);
        if (metadata.hasError() || !metadata.isVersion(version)) {
            // needs updating
            metadata = new AppMetadata()
                    .setActive_version(version)
                    .setInstalled_by("cloudos-builtin");
            metadata.write(appDir);
        }

        log.info("*** Successfully registered builtin app: "+app+"/"+version);
    }

}
