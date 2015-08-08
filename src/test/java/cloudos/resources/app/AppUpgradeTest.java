package cloudos.resources.app;

import cloudos.appstore.model.AppStoreAccount;
import cloudos.appstore.model.CloudAppStatus;
import cloudos.appstore.model.app.AppManifest;
import cloudos.model.auth.ApiToken;
import cloudos.appstore.model.support.AppInstallStatus;
import cloudos.appstore.model.support.AppListing;
import cloudos.appstore.model.CloudAppVersion;
import cloudos.appstore.test.AppStoreTestUtil;
import cloudos.appstore.test.TestApp;
import cloudos.server.CloudOsConfiguration;
import org.cobbzilla.wizard.task.TaskResult;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.RestServer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.junit.Assert.assertEquals;

public class AppUpgradeTest extends AppTestBase {

    private static final String DOC_TARGET = "App Upgrading";

    public static final String MANIFEST_RESOURCE_PATH = "apps/noconfig-webapp-manifest.json";
    public static final String UPGRADED_MANIFEST_RESOURCE_PATH = "apps/noconfig-upgrade-manifest.json";
    public static final String TEST_ICON = "apps/some-icon.png";

    private static TestApp testApp;
    private static TestApp upgradedApp;

    private ApiToken token;
    private AppStoreAccount publisher;
    private CloudAppVersion version;
    private CloudAppVersion upgradedVersion;

    @BeforeClass
    public static void setupTestWebApp() throws Exception {
        testApp = webServer.buildAppBundle(MANIFEST_RESOURCE_PATH, null, TEST_ICON);
    }

    @Override public void onStart(RestServer<CloudOsConfiguration> server) {
        try { initAppStore(); } catch (Exception e) {
            die("Error initializing app store: " + e, e);
        }
        super.onStart(server);
    }

    /**
     * populate app store with a single app and a single version
     */
    private void initAppStore() throws Exception {
        // register an app publisher
        token = AppStoreTestUtil.registerPublisher(appStoreClient);
        appStoreClient.pushToken(token.getToken());
        publisher = appStoreClient.findAccount();

        // as the publisher, create an app and an active appVersion
        version = AppStoreTestUtil.newCloudApp(appStoreClient, publisher.getName(), testApp.getBundleUrl(), testApp.getBundleUrlSha());

        publishApp(version);
    }

    private void publishApp(CloudAppVersion version) throws Exception {
        appStoreClient.pushToken(adminToken);
        appStoreClient.updateAppStatus(publisher.getName(), version.getApp(), version.getVersion(), CloudAppStatus.published);
        appStoreClient.popToken();
    }

    @Test public void testAppUpgrade () throws Exception {

        String expectedVersion = version.getVersion();
        apiDocs.startRecording(DOC_TARGET, "upgrade an app to the next version");

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as available");
        expectVersionAndStatus(expectedVersion, AppInstallStatus.available_appstore);

        // download/install initial version
        apiDocs.addNote("download the 'noconfig' app to local app repository...");
        TaskResult result = downloadApp(testApp.getBundleUrl());

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as " + AppInstallStatus.available_local);
        expectVersionAndStatus(expectedVersion, AppInstallStatus.available_local);

        apiDocs.addNote("install the 'noconfig' app (does not require config)...");
        AppManifest manifest = AppManifest.fromJson(result.getReturnValue());
        installApp(manifest);

        apiDocs.addNote("query app store, should see app version "+ expectedVersion +" as installed");
        expectVersionAndStatus(expectedVersion, AppInstallStatus.installed);

        // update app in app store to new version
        apiDocs.addNote("...behind the scenes, update app store with new version, and publish the new version...");
        upgradedApp = webServer.buildAppBundle(UPGRADED_MANIFEST_RESOURCE_PATH, null, TEST_ICON);

        // as the publisher, create an app and an active appVersion
        upgradedVersion = AppStoreTestUtil.newCloudApp(appStoreClient, publisher.getName(), upgradedApp.getBundleUrl(), upgradedApp.getBundleUrlSha());
        publishApp(upgradedVersion);
        expectedVersion = upgradedVersion.getVersion();

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as " + AppInstallStatus.upgrade_available_installed);
        expectVersionAndStatus(expectedVersion, AppInstallStatus.upgrade_available_installed);

        // download upgraded version
        apiDocs.addNote("download the upgraded version of 'noconfig' to local app repository...");
        result = downloadApp(upgradedApp.getBundleUrl());

        apiDocs.addNote("query app store, should see app version " + expectedVersion + " as " + AppInstallStatus.upgrade_available_installed);
        expectVersionAndStatus(expectedVersion, AppInstallStatus.upgrade_available_installed);

        apiDocs.addNote("install the upgraded version of 'noconfig' app (does not require config)...");
        manifest = AppManifest.fromJson(result.getReturnValue());
        installApp(manifest);

        apiDocs.addNote("query app store, should see app version "+ expectedVersion +" as installed");
        expectVersionAndStatus(expectedVersion, AppInstallStatus.installed);
    }

    public void expectVersionAndStatus(String expectedVersion, AppInstallStatus status) throws Exception {
        SearchResults<AppListing> results;
        AppListing appListing;
        results = queryAppStore();
        assertEquals(1, results.total());
        appListing = results.getResult(0);
        assertEquals(version.getApp(), appListing.getName());
        assertEquals(expectedVersion, appListing.getVersion());
        assertEquals(status, appListing.getInstallStatus());
    }

}
