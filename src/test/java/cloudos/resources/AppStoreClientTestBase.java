package cloudos.resources;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.AppStoreAccount;
import cloudos.appstore.test.AppStoreTestUser;
import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.cobbzilla.restex.RestexClientConnectionManager;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.junit.Before;

import java.util.UUID;

public class AppStoreClientTestBase extends ApiClientTestBase {

    protected static TemplateCaptureTarget appStoreApiDocs = new TemplateCaptureTarget("target/appstore-api-examples");
    @Getter protected HttpClient appStoreHttpClient = new RestexClientConnectionManager(appStoreApiDocs).getHttpClient();

    protected static AppStoreApiClient appStoreClient;

    protected String adminToken;
    protected AppStoreAccount admin;

    @Before
    public void createAppStoreAdmin () throws Exception {
        // fixme: use mock object for app store client
        final AppStoreTestUser adminUser = new AppStoreTestUser(UUID.randomUUID().toString(), new AppStoreAccount());
        adminToken = adminUser.getToken();
        admin = adminUser.getAccount();
        appStoreClient = null;
    }

}
