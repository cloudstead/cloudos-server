package cloudos.resources;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.AppStoreAccount;
import cloudos.appstore.server.AppStoreApiConfiguration;
import cloudos.appstore.server.AppStoreApiServer;
import cloudos.appstore.test.AppStoreTestUser;
import cloudos.appstore.test.AppStoreTestUtil;
import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.cobbzilla.restex.RestexClientConnectionManager;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.junit.After;
import org.junit.Before;

public class AppStoreClientTestBase extends ApiClientTestBase {

    private static final String APPSTORE_TEST_CONFIG = "appstore/appstore-config-test.yml";

    protected static TemplateCaptureTarget appStoreApiDocs = new TemplateCaptureTarget("target/appstore-api-examples");
    @Getter protected HttpClient appStoreHttpClient = new RestexClientConnectionManager(appStoreApiDocs).getHttpClient();

    protected static RestServerHarness<AppStoreApiConfiguration, AppStoreApiServer> appStoreHarness;
    protected static AppStoreApiConfiguration appStoreConfiguration;
    protected static AppStoreApiClient appStoreClient;

    protected String adminToken;
    protected AppStoreAccount admin;

    @Before
    public void createAppStoreAdmin () throws Exception {
        final AppStoreTestUser adminUser = AppStoreTestUtil.createAdminUser(appStoreClient, appStoreHarness.getServer());
        adminToken = adminUser.getToken();
        admin = adminUser.getAccount();
    }

    @Before
    public void runAppStore () throws Exception {
        if (appStoreHarness == null) {
            appStoreHarness = new RestServerHarness<>(AppStoreApiServer.class);
            appStoreHarness.setConfigurations(StreamConfigurationSource.fromResources(getClass(), APPSTORE_TEST_CONFIG));
            appStoreHarness.startServer();
        }
        appStoreConfiguration = appStoreHarness.getServer().getConfiguration();
        appStoreClient = new AppStoreApiClient(appStoreHarness.getServer().getClientUri(), appStoreHttpClient);
    }

    @After public void stopAppStore () throws Exception { appStoreHarness.stopServer(); }

}
