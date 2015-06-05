package cloudos.resources;

import cloudos.appstore.model.CloudApp;
import cloudos.appstore.model.CloudAppVersion;
import cloudos.appstore.model.app.AppLevel;
import cloudos.appstore.model.support.AppListing;
import cloudos.appstore.model.support.AppStoreQuery;
import cloudos.server.CloudOsConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.http.ApiConnectionInfo;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static cloudos.appstore.ApiConstants.APPS_ENDPOINT;
import static org.apache.commons.lang3.StringUtils.chop;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.junit.Assert.*;

public class AppStoreResourceTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "App Store";

    public static final int NUM_APPS = 40;
    public static final int NUM_SYSTEM_APPS = 20;

    private List<QuickApp> apps = new ArrayList<>();
    private List<QuickApp> systemApps = new ArrayList<>();

    @Override public void onStart(RestServer<CloudOsConfiguration> server) {
        super.onStart(server);

        final String publisherName = appStoreClient.getConnectionInfo().getUser();

        for (int i=0; i<NUM_APPS; i++) {
            apps.add(quickCloudApp(publisherName, AppLevel.app));
        }
        for (int i=0; i<NUM_SYSTEM_APPS; i++) {
            systemApps.add(quickCloudApp(publisherName, AppLevel.system));
        }
    }

    @Test public void testQueryAppStore () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "query the app store");

        SearchResults<AppListing> results;

        apiDocs.addNote("query first page of apps (with page size of 10)");
        results = queryAppStore(new AppStoreQuery().setPageSize(10));
        assertEquals(results.count(), 10);
        assertEquals(results.total(), NUM_APPS);

        apiDocs.addNote("query third page of apps (with page size of 12)");
        results = queryAppStore(new AppStoreQuery().setPageNumber(3).setPageSize(12));
        assertEquals(results.count(), 12);
        assertEquals(results.total(), NUM_APPS);

        apiDocs.addNote("query ALL apps (with MAX_INT page size)");
        results = queryAppStore(new AppStoreQuery().setPageSize(AppStoreQuery.INFINITE));
        assertEquals(results.count(), NUM_APPS);
        assertEquals(results.total(), NUM_APPS);

        apiDocs.addNote("query ALL 'system' apps");
        results = queryAppStore(new AppStoreQuery().setLevel(AppLevel.system).setPageSize(AppStoreQuery.INFINITE));
        assertEquals(results.count(), NUM_SYSTEM_APPS);
        assertEquals(results.total(), NUM_SYSTEM_APPS);

        String name = apps.get(0).app.getName();
        final int pos = name.indexOf("-");
        name = name.substring(0, pos);
        apiDocs.addNote("query apps for '"+ name +"', should be at least one result");
        results = queryAppStore(new AppStoreQuery().setFilter(name));
        assertTrue(results.count() > 0);
        assertTrue(results.total() > 0);

        name = RandomStringUtils.randomAlphanumeric(6)+"-"+System.currentTimeMillis();
        apiDocs.addNote("query apps for '"+name+"', should not find anything");
        final RestResponse response = postQuery(new AppStoreQuery().setFilter(name));
        assertEquals(response.status, HttpStatusCodes.NOT_FOUND);
    }

    @Test public void testAppstoreApiProxyCalls() throws Exception {

        apiDocs.startRecording(DOC_TARGET, "Call the 'native' AppStore API via the CloudOs API proxy");

        RestResponse response;
        final String publisherName = appStoreClient.getConnectionInfo().getUser();
        final String name = apps.get(0).app.getName();
        final String version = apps.get(0).app.getVersion();

        // use alternate base uri -- this relays to app store (and injects appropriate credentials)
        final ApiConnectionInfo info = new ApiConnectionInfo(appstoreProxyBase(), admin.getName(), admin.getPassword());

        // call getHttpClient, ensures traffic is captured via restex
        // override getTokenHeader to send the cloudos auth token
        final ApiClientBase appStoreApi = new ApiClientBase(info, getHttpClient()) {
            @Override protected String getTokenHeader() { return ApiConstants.H_API_KEY; }
        };
        appStoreApi.setToken(adminToken);

        apiDocs.addNote("Query for details about a particular app");
        response = appStoreApi.get(APPS_ENDPOINT + "/" + publisherName + "/" + name);
        final CloudApp app = fromJson(response.json, CloudApp.class);
        assertNotNull(app);

        apiDocs.addNote("Query for details about a particular app version");
        response = appStoreApi.get(APPS_ENDPOINT + "/" + publisherName + "/" + name + "/" + version);
        final CloudAppVersion appVersion = fromJson(response.json, CloudAppVersion.class);
        assertNotNull(appVersion);
    }

    protected String appstoreProxyBase() {

        final String publicBase = getConfiguration().getPublicUriBase();

        // for now there is only one custom http handler. we'll need to update this if we add more
        // (or just make sure the appstore API proxy handler remains first in the list)
        final String appStoreUri = getConfiguration().getHandlers()[0].getUri();

        // be somewhat smart about avoiding double-slashes in url
        return (publicBase.endsWith("/") && appStoreUri.startsWith("/"))
            ? chop(publicBase) + appStoreUri
            : publicBase + appStoreUri;
    }
}
