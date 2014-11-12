package cloudos.resources;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.support.ApiToken;
import cloudos.appstore.model.support.AppListing;
import cloudos.appstore.model.support.AppStoreAccountRegistration;
import cloudos.appstore.test.AppStoreSeedData;
import cloudos.appstore.test.AppStoreTestUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.junit.Test;

import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertEquals;

public class AppStoreResourceTest extends ApiClientTestBase {

    public static final String DOC_TARGET = "App Store";

    public static final int NUM_ACCOUNTS = 2;
    public static final int NUM_APPS = 8;
    public static final int NUM_VERSIONS = 2;

    protected AppStoreSeedData seedData;

    @Override public void onStart() {
        final AppStoreApiClient appStoreClient = getConfiguration().getAppStoreClient();
        try {
            final ApiToken apiToken = appStoreClient.registerAccount((AppStoreAccountRegistration) AppStoreTestUtil.buildPublisherRegistration().setAdmin(true));
            appStoreClient.setToken(apiToken.getToken());
            seedData = new AppStoreSeedData(appStoreClient, adminToken, NUM_ACCOUNTS, NUM_APPS, NUM_VERSIONS);
        } catch (Exception e) {
            throw new IllegalStateException("error populating seed data: "+e, e);
        }
    }

    @Test
    public void testAppStoreQuery() throws Exception {

        apiDocs.startRecording(DOC_TARGET, "default app store query");

        apiDocs.addNote("search app store with some queries");
        final SearchResults<AppListing> results = JsonUtil.fromJson(post(ApiConstants.APPSTORE_ENDPOINT, toJson(ResultPage.DEFAULT_PAGE)).json, SearchResults.jsonType(AppListing.class));
        assertEquals(NUM_APPS, results.size());
    }

}
