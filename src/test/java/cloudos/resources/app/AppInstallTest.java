package cloudos.resources.app;

import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.model.Account;
import cloudos.model.app.AppConfiguration;
import cloudos.service.task.TaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.io.File;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;
import static cloudos.resources.ApiConstants.SESSIONS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;

@Slf4j
public class AppInstallTest extends AppTestBase {

    private static final String DOC_TARGET = "App Installation";
    public static final String MANIFEST_RESOURCE_PATH = "apps/simple-webapp-manifest.json";

    @BeforeClass public static void setupTestWebApp() throws Exception { setupTestWebApp(MANIFEST_RESOURCE_PATH); }

    @Test
    public void testInstallApp () throws Exception {

        TaskResult result;

        apiDocs.startRecording(DOC_TARGET, "Download, configure and install an application");

        apiDocs.addNote("download the 'test' app");
        result = downloadApp(bundleUrl);

        // Validate that default taskbar icon image was downloaded and installed
        final File installedIcon = new File(new AppLayout(appRepository, "simple-webapp", "0.1.0").getChefFilesDir(), "taskbarIcon.png");
        assertTrue(installedIcon.exists());

        final AppManifest manifest = fromJson(result.getReturnValue(), AppManifest.class);
        final String configUri = APPS_ENDPOINT + "/apps/" + manifest.getScrubbedName() + "/versions/" + manifest.getScrubbedVersion() + "/config";
        AppConfiguration appConfig;

        apiDocs.addNote("read configuration information for the app");
        appConfig = fromJson(doGet(configUri).json, AppConfiguration.class);
        assertEquals(2, appConfig.getCategories().size());
        assertEquals("init", appConfig.getCategories().get(0).getName());
        assertEquals(5, appConfig.getCategory("init").getItems().size());
        assertEquals("custom", appConfig.getCategories().get(1).getName());
        assertEquals(2, appConfig.getCategory("custom").getItems().size());
        assertTrue(appConfig.getCategory("init").getValues().isEmpty());
        assertTrue(appConfig.getCategory("custom").getValues().isEmpty());

        final String rand = RandomStringUtils.randomAlphanumeric(10);
        appConfig.getCategory("init").set("admin.name", rand);
        appConfig.getCategory("init").set("admin.password", rand);
        appConfig.getCategory("init").set("test.config1", rand);
        appConfig.getCategory("init").set("test.config2", rand);
        appConfig.getCategory("init").set("test.config3", rand);
        appConfig.getCategory("custom").set("c1", "custom-" + rand);
        appConfig.getCategory("custom").set("c2", "custom-" + rand);

        apiDocs.addNote("write configuration information for the app");
        assertEquals(200, doPost(configUri, toJson(appConfig)).status);

        apiDocs.addNote("re-read config, ensure settings were written");
        appConfig = fromJson(doGet(configUri).json, AppConfiguration.class);
        assertEquals(rand, appConfig.getCategory("init").get("admin.name"));
        assertEquals(rand, appConfig.getCategory("init").get("admin.password"));
        assertEquals("custom-" + rand, appConfig.getCategory("custom").get("c1"));
        assertEquals("custom-"+rand, appConfig.getCategory("custom").get("c2"));

        // Install the app and verify chef message was sent
        installApp(manifest);

        // Verify app was installed correctly
        final String appDatabagsDir = chefHandler.getChefDir() + "/data_bags/" + manifest.getScrubbedName();
        final JsonNode initDatabag = fromJson(FileUtil.toString(appDatabagsDir + "/init.json"), JsonNode.class);
        assertEquals(rand, JsonUtil.nodeValue(initDatabag, "test.config1"));

        final JsonNode customDatabag = fromJson(FileUtil.toString(chefHandler.getChefDir() + "/data_bags/" + manifest.getScrubbedName() + "/custom.json"), JsonNode.class);
        assertEquals("custom-"+rand, JsonUtil.nodeValue(customDatabag, "c2"));

        assertTrue(new File(appDatabagsDir+"/cloudos-manifest.json").exists());

        // refetch session, should now have updated list of apps
        apiDocs.addNote("re-fetch the session info for the account, availableApps should now include the app we just installed");
        final Account account = fromJson(get(SESSIONS_ENDPOINT + "/" + token).json, Account.class);
        final AppRuntimeDetails details = findRuntime(account, "simple-webapp");
        assertNotNull("simple-webapp not found", details);

        apiDocs.addNote("request the taskbarIcon for the app, this should now work");
        final HttpResponseBean responseBean = HttpUtil.getResponse(details.getAssets().getTaskbarIconUrl());
        assertEquals(200, responseBean.getStatus());
        assertEquals(iconFile.length(), Long.parseLong(responseBean.getFirstHeaderValue(HttpHeaders.CONTENT_LENGTH)));
    }

    private AppRuntimeDetails findRuntime(Account account, String name) {
        for (AppRuntimeDetails details : account.getAvailableApps()) {
            if (details.getName().equals(name)) return details;
        }
        return null;
    }

}
