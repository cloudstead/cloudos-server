package cloudos.resources.app;

import cloudos.appstore.bundler.BundlerMain;
import cloudos.appstore.bundler.BundlerOptions;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.model.Account;
import cloudos.model.app.AppConfiguration;
import cloudos.model.support.AppDownloadRequest;
import cloudos.resources.ApiClientTestBase;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.util.system.PortPicker;
import org.cobbzilla.wizard.util.RestResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rooty.toots.chef.ChefMessage;

import javax.ws.rs.core.HttpHeaders;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;

@Slf4j
public class AppInstallTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "App Installation";
    private static final String TEST_APP_TARBALL = "test-bundle.tar.gz";

    private static final String TEST_MANIFEST = "apps/simple-webapp-manifest.json";
    private static final String TEST_ICON = "apps/some-icon.png";
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private static Server testHttpServer;
    private static int testServerPort;
    private static File testDocRoot;
    private static File iconFile;

    @BeforeClass public static void setupTestWebApp() throws Exception {

        testServerPort = PortPicker.pick();

        // Write manifest for test app to a temp dir
        final File appTemp = FileUtil.createTempDir("appTemp");
        final File manifestFile = new File(appTemp, AppManifest.CLOUDOS_MANIFEST_JSON);
        final String manifestData = StreamUtil.loadResourceAsString(TEST_MANIFEST).replace("@@PORT@@", String.valueOf(testServerPort));
        FileUtil.toFile(manifestFile, manifestData);

        testDocRoot = FileUtil.createTempDir(AppInstallTest.class.getName());
        File bundleDir = new File(testDocRoot, "scratch");

        // Run the bundler on our test manifest
        final BundlerMain main = new BundlerMain(new String[] {
                BundlerOptions.OPT_MANIFEST, manifestFile.getAbsolutePath(),
                BundlerOptions.OPT_OUTPUT_DIR, bundleDir.getAbsolutePath()
        });
        main.run();

        // Roll the tarball into its place under the doc root
        CommandShell.exec(new CommandLine("tar")
                .addArgument("czf")
                .addArgument(testDocRoot.getAbsolutePath()+"/"+TEST_APP_TARBALL)
                .addArgument("."), bundleDir);

        // Copy icon png to doc root
        iconFile = new File(testDocRoot, new File(TEST_ICON).getName());
        FileUtils.copyFile(StreamUtil.loadResourceAsFile(TEST_ICON), iconFile);

        // Set up jetty server to serve tarball and icon png
        testHttpServer = new Server(testServerPort);

        final ResourceHandler handler = new ResourceHandler();
        handler.setResourceBase(testDocRoot.getAbsolutePath());
        testHttpServer.setHandler(handler);

        testHttpServer.start();
    }

    @AfterClass public static void teardownTestApp () throws Exception {
        testHttpServer.stop();
        FileUtils.deleteDirectory(testDocRoot);
    }

    @Test
    public void testInstallApp () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "Download, configure and install an application");

        apiDocs.addNote("download the 'test' app");

        // Download the app
        final AppDownloadRequest downloadRequest = new AppDownloadRequest()
                .setUrl("http://127.0.0.1:"+testServerPort+"/"+TEST_APP_TARBALL)
                .setToken(randomPassword());
        apiDocs.addNote("initiate the download request to add the app to app-repository");
        final RestResponse response = doPost(APPS_ENDPOINT + "/download", toJson(downloadRequest));
        assertEquals(200, response.status);
        TaskId taskId = fromJson(response.json, TaskId.class);
        TaskResult result = getTaskResult(taskId);

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

        // Install the app
        apiDocs.addNote("initiate the install request to install from app-repository to main chef-solo");
        final String installUri = APPS_ENDPOINT + "/apps/"+manifest.getScrubbedName()+"/versions/"+manifest.getScrubbedVersion()+"/install";
        final String json = doPost(installUri, null).json;
        taskId = fromJson(json, TaskId.class);
        result = getTaskResult(taskId);

        // Ensure rooty message was sent
        final ChefMessage chefMessage = getRootySender().first(ChefMessage.class);
        assertNotNull(chefMessage);
        assertEquals(1, chefMessage.getCookbooks().size());
        assertEquals(manifest.getScrubbedName(), chefMessage.getCookbooks().get(0));

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

    private TaskResult getTaskResult(TaskId taskId) throws Exception {
        long start = System.currentTimeMillis();
        TaskResult result = null;
        while (System.currentTimeMillis() - start < TIMEOUT) {
            Thread.sleep(250);
            apiDocs.addNote("check status of task " + taskId.getUuid());
            final String json = doGet(TASKS_ENDPOINT + "/" + taskId.getUuid()).json;
            result = fromJson(json, TaskResult.class);
            if (result.isSuccess()) break;
        }
        log.info("getTaskResult: "+result);
        assertNotNull(result);
        assertTrue(result.isSuccess());
        return result;
    }

}
