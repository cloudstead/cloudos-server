package cloudos.resources.app;

import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.config.AppConfigMetadataDatabag;
import cloudos.appstore.model.app.config.AppConfigTranslationCategory;
import cloudos.appstore.model.app.config.AppConfiguration;
import cloudos.appstore.test.TestApp;
import cloudos.model.Account;
import cloudos.service.task.TaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.util.http.HttpUtil;
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

    public static final String TEST_CONFIG_MANIFEST = "apps/simple-webapp-config-metadata.json";
    public static final String TEST_MANIFEST = "apps/simple-webapp-manifest.json";
    public static final String TEST_ICON = "apps/some-icon.png";

    private static TestApp testApp;

    @BeforeClass public static void setupTestWebApp() throws Exception {
        testApp = webServer.buildAppTarball(TEST_MANIFEST, TEST_CONFIG_MANIFEST, TEST_ICON);
    }

    @Test public void testInstallApp () throws Exception {

        TaskResult result;

        apiDocs.startRecording(DOC_TARGET, "Download, configure and install an application");

        apiDocs.addNote("download the 'test' app");
        result = downloadApp(testApp.getBundleUrl());

        // Validate that default taskbar icon image was downloaded and installed
        final File installedIcon = new File(new AppLayout(appRepository, "simple-webapp", "0.1.0").getChefFilesDir(), "taskbarIcon.png");
        assertTrue(installedIcon.exists());

        final AppManifest manifest = AppManifest.fromJson(result.getReturnValue());
        final String configUri = APPS_ENDPOINT + "/apps/" + manifest.getScrubbedName() + "/versions/" + manifest.getScrubbedVersion() + "/config";
        AppConfiguration appConfig;

        apiDocs.addNote("read configuration information for the app");
        appConfig = fromJson(doGet(configUri).json, AppConfiguration.class);
        assertEquals(2, appConfig.getCategories().size());
        assertEquals("init", appConfig.getCategories().get(0).getName());
        assertEquals(6, appConfig.getCategory("init").getItems().size());
        assertEquals("custom", appConfig.getCategories().get(1).getName());
        assertEquals(3, appConfig.getCategory("custom").getItems().size());
        assertTrue(appConfig.getCategory("init").getValues().isEmpty());
        assertTrue(appConfig.getCategory("custom").getValues().isEmpty());

        // ensure metadata and locale fields were populated appropriately
        assertTrue(appConfig.getTranslations().getCategories().containsKey("init"));
        final AppConfigTranslationCategory init = appConfig.getTranslations().getCategories().get("init");
        final AppConfigMetadataDatabag initConfig = appConfig.getMetadata().getCategories().get("init");
        assertFalse(initConfig.isAdvanced());
        for (String choice : initConfig.getFields().get("test.loc1").getChoices()) {
            assertNotNull(init.get("test.loc1.choice."+choice));
        }

        assertTrue(appConfig.getTranslations().getCategories().containsKey("custom"));
        final AppConfigTranslationCategory custom = appConfig.getTranslations().getCategories().get("custom");
        final AppConfigMetadataDatabag customConfig = appConfig.getMetadata().getCategories().get("custom");
        assertTrue(customConfig.isAdvanced());
        for (String choice : customConfig.getFields().get("loc2").getChoices()) {
            assertNotNull(custom.get("loc2.choice."+choice));
        }

        final String rand = RandomStringUtils.randomAlphanumeric(10);
        appConfig.getCategory("init").set("admin.name", rand);
        appConfig.getCategory("init").set("admin.password", rand);
        appConfig.getCategory("init").set("test.config1", rand);
        appConfig.getCategory("init").set("test.config2", rand);
        appConfig.getCategory("init").set("test.config3", rand);
        appConfig.getCategory("init").set("test.loc1", "en");
        appConfig.getCategory("custom").set("c1", "custom-" + rand);
        appConfig.getCategory("custom").set("c2", "custom-" + rand);
        appConfig.getCategory("custom").set("loc2", "zh_TW");

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
        final JsonNode initDatabag = fromJson(new File(appDatabagsDir, "init.json"), JsonNode.class);
        assertEquals(rand, JsonUtil.nodeValue(initDatabag, "test.config1"));

        final JsonNode customDatabag = fromJson(new File(chefHandler.getChefDir() + "/data_bags/" + manifest.getScrubbedName() + "/custom.json"), JsonNode.class);
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
        assertEquals(testApp.getIconFile().length(), Long.parseLong(responseBean.getFirstHeaderValue(HttpHeaders.CONTENT_LENGTH)));
    }

    private AppRuntimeDetails findRuntime(Account account, String name) {
        for (AppRuntimeDetails details : account.getAvailableApps()) {
            if (details.getName().equals(name)) return details;
        }
        return null;
    }

}
