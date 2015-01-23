package cloudos.resources.app;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.AppStoreAccount;
import cloudos.appstore.model.CloudApp;
import cloudos.appstore.model.CloudAppStatus;
import cloudos.appstore.model.CloudAppVersion;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.support.ApiToken;
import cloudos.appstore.model.support.AppInstallStatus;
import cloudos.appstore.model.support.AppListing;
import cloudos.appstore.test.AppStoreTestUtil;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.TaskResult;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.RestServer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.junit.Assert.assertEquals;

public class AppUpgradeTest extends AppTestBase {

    private static final String DOC_TARGET = "App Upgrading";
    public static final String MANIFEST_RESOURCE_PATH = "apps/noconfig-webapp-manifest.json";
    public static final String UPGRADED_MANIFEST_RESOURCE_PATH = "apps/noconfig-upgrade-manifest.json";

    private ApiToken token;
    private AppStoreAccount publisher;
    private CloudApp app;
    private CloudAppVersion appVersion;

    @BeforeClass public static void setupTestWebApp() throws Exception { setupTestWebApp(MANIFEST_RESOURCE_PATH); }

    @Override public void onStart(RestServer<CloudOsConfiguration> server) {
        try { initAppStore(); } catch (Exception e) {
            throw new IllegalStateException("Error initializing app store: "+e, e);
        }
        super.onStart(server);
    }

    /**
     * populate app store with a single app and a single version
     */
    private void initAppStore() throws Exception {
        // register an app publisher
        final AppStoreApiClient appStoreClient = getAppStoreClient();
        token = AppStoreTestUtil.registerPublisher(appStoreClient);
        appStoreClient.pushToken(token.getToken());
        publisher = appStoreClient.findAccount();

        // as the publisher, create an app and an active appVersion
        app = AppStoreTestUtil.newCloudApp(appStoreClient, publisher.getUuid(), appManifest.getName());
        defineAppVersion();
    }

    private void defineAppVersion() throws Exception {
        appVersion = AppStoreTestUtil.buildCloudAppVersion(app);
        appVersion.setVersion(appManifest.getVersion());
        appVersion.setBundleUrl(bundleUrl);
        appVersion.setBundleUrlSha(bundleUrlSha);
        getAppStoreClient().defineAppVersion(appVersion);

        // publish the appVersion
        appVersion.setAppStatus(CloudAppStatus.PUBLISHED);
        getAppStoreClient().updateAppVersion(appVersion);
    }

    @Test public void testAppUpgrade () throws Exception {

        String expectedVersion = appManifest.getVersion();
        apiDocs.startRecording(DOC_TARGET, "upgrade an app to the next version");

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as available");
        expectVersionAndStatus(expectedVersion, AppInstallStatus.available_appstore);

        // download/install initial version
        apiDocs.addNote("download the 'noconfig' app to local app repository...");
        TaskResult result = downloadApp(bundleUrl);

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as " + AppInstallStatus.available_local);
        expectVersionAndStatus(expectedVersion, AppInstallStatus.available_local);

        apiDocs.addNote("install the 'noconfig' app (does not require config)...");
        AppManifest manifest = fromJson(result.getReturnValue(), AppManifest.class);
        installApp(manifest);

        apiDocs.addNote("query app store, should see app version "+ expectedVersion +" as installed");
        expectVersionAndStatus(expectedVersion, AppInstallStatus.installed);

        // update app in app store to new version
        apiDocs.addNote("...behind the scenes, update app store with new version, and publish the new version...");
        buildAppTarball(UPGRADED_MANIFEST_RESOURCE_PATH);
        defineAppVersion();
        expectedVersion = appManifest.getVersion();

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as " + AppInstallStatus.upgrade_available_installed);
        expectVersionAndStatus(expectedVersion, AppInstallStatus.upgrade_available_installed);

        // download upgraded version
        apiDocs.addNote("download the upgraded version of 'noconfig' to local app repository...");
        result = downloadApp(bundleUrl);

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as " + AppInstallStatus.upgrade_available_installed);
        expectVersionAndStatus(expectedVersion, AppInstallStatus.upgrade_available_installed);

        apiDocs.addNote("install the upgraded version of 'noconfig' app (does not require config)...");
        manifest = fromJson(result.getReturnValue(), AppManifest.class);
        installApp(manifest);

        apiDocs.addNote("query app store, should see app version "+ expectedVersion +" as installed");
        expectVersionAndStatus(expectedVersion, AppInstallStatus.installed);
    }

    public void expectVersionAndStatus(String expectedVersion, AppInstallStatus status) throws Exception {
        SearchResults<AppListing> results;
        AppListing appListing;
        results = queryAppStore();
        assertEquals(1, results.size());
        appListing = results.getResult(0);
        assertEquals(app.getName(), appListing.getName());
        assertEquals(expectedVersion, appListing.getAppVersion().getVersion());
        assertEquals(status, appListing.getInstallStatus());
    }

}
