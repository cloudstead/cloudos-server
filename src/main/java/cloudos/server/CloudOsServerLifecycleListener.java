package cloudos.server;

import cloudos.appstore.model.app.AppManifest;
import cloudos.model.app.AppMetadata;
import cloudos.model.app.CloudOsAppLayout;
import com.fasterxml.jackson.core.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListener;
import rooty.toots.chef.ChefHandler;
import rooty.toots.chef.ChefMessage;
import rooty.toots.chef.ChefSolo;

import java.io.File;

@Slf4j
public class CloudOsServerLifecycleListener implements RestServerLifecycleListener<CloudOsConfiguration> {

    @Override public void onStart(RestServer<CloudOsConfiguration> server) {}
    @Override public void beforeStop(RestServer<CloudOsConfiguration> server) {}
    @Override public void onStop(RestServer<CloudOsConfiguration> server) {}

    /**
     * Read authoritative solo.json, populate app-repository if we are missing anything that is already installed
     * @param server The CloudOsServer
     */
    @Override public void beforeStart(RestServer<CloudOsConfiguration> server) {
        if (true) return; // disable for now
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
                        final File manifestFile = new File(chefDir.getAbsolutePath()+"/data_bags/"+app+"/"+AppManifest.CLOUDOS_MANIFEST_JSON);
                        if (manifestFile.exists()) {
                            // Load the manifest
                            final AppManifest manifest = AppManifest.load(manifestFile);

                            // Does the app-repository contain this app+version?
                            final CloudOsAppLayout layout = server.getConfiguration().getAppLayout();
                            final File appVersionDir = layout.getAppVersionDir(manifest.getName(), manifest.getVersion());
                            if (!appVersionDir.exists()) {
                                registerApp(server, manifestFile, manifest, chefDir);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error inspecting recipe "+recipe+" while looking for apps: "+e);
            }
        }
    }

    /**
     * Called when an app is found in the chef-solo repo, but not in the cloudos app-repository.
     * @param server The CloudOsServer
     * @param manifestFile The manifest file in the main chef-solo directory
     * @param manifest The manifest found in the chef-solo databags dir for this app
     * @param chefDir The main chef solo directory
     */
    private void registerApp(RestServer<CloudOsConfiguration> server, File manifestFile, AppManifest manifest, File chefDir) throws Exception {

        final String app = manifest.getScrubbedName();
        final String version = manifest.getVersion();

        final CloudOsAppLayout layout = server.getConfiguration().getAppLayout();
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
