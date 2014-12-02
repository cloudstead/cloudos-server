package cloudos.resources.app;

import cloudos.appstore.bundler.BundlerMain;
import cloudos.appstore.bundler.BundlerOptions;
import cloudos.appstore.model.app.AppManifest;
import cloudos.model.app.AppConfiguration;
import cloudos.model.support.AppDownloadRequest;
import cloudos.resources.ApiClientTestBase;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
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

import java.io.File;
import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;
import static cloudos.resources.ApiConstants.TASKS_ENDPOINT;
import static cloudos.resources.ApiConstants.randomPassword;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AppInstallTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "App Installation";
    private static final String TEST_APP_TARBALL = "test-bundle.tar.gz";

    private static final String TEST_MANIFEST = "apps/simple-webapp-manifest.json";
    private static final long TIMEOUT = TimeUnit.HOURS.toMillis(10);

    private static Server testHttpServer;
    private static int testServerPort;
    private static File testDocRoot;

    @BeforeClass public static void setupTestWebApp() throws Exception {

        // Write manifest for test app to a temp dir
        final File appTemp = FileUtil.createTempDir("appTemp");
        final File manifestFile = new File(appTemp, AppManifest.CLOUDOS_MANIFEST_JSON);
        FileUtil.writeResourceToFile(TEST_MANIFEST, manifestFile, AppInstallTest.class);

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

        // Set up jetty server to serve tarball
        testServerPort = PortPicker.pick();
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
        final RestResponse response = doPost(APPS_ENDPOINT + "/download", toJson(downloadRequest));
        assertEquals(200, response.status);
        TaskId taskId = fromJson(response.json, TaskId.class);
        TaskResult result = getTaskResult(taskId);

        // Read configuration for the app
        final AppManifest manifest = fromJson(result.getReturnValue(), AppManifest.class);
        final String configUri = APPS_ENDPOINT + "/apps/" + manifest.getScrubbedName() + "/versions/" + manifest.getScrubbedVersion() + "/config";
        AppConfiguration appConfig;

        appConfig = fromJson(doGet(configUri).json, AppConfiguration.class);
        assertEquals(2, appConfig.getCategories().size());
        assertEquals("init", appConfig.getCategories().get(0).getName());
        assertEquals(5, appConfig.getCategory("init").getItems().size());
        assertEquals("custom", appConfig.getCategories().get(1).getName());
        assertEquals(2, appConfig.getCategory("custom").getItems().size());
        assertTrue(appConfig.getCategory("init").getValues().isEmpty());
        assertTrue(appConfig.getCategory("custom").getValues().isEmpty());

        // Write configuration for the app
        final String rand = RandomStringUtils.randomAlphanumeric(10);
        appConfig.getCategory("init").set("admin.name", rand);
        appConfig.getCategory("init").set("admin.password", rand);
        appConfig.getCategory("init").set("test.config1", rand);
        appConfig.getCategory("init").set("test.config2", rand);
        appConfig.getCategory("init").set("test.config3", rand);
        appConfig.getCategory("custom").set("c1", "custom-"+rand);
        appConfig.getCategory("custom").set("c2", "custom-"+rand);

        assertEquals(200, doPost(configUri, toJson(appConfig)).status);

        // Re-read config, ensure settings were written
        appConfig = fromJson(doGet(configUri).json, AppConfiguration.class);
        assertEquals(rand, appConfig.getCategory("init").get("admin.name"));
        assertEquals(rand, appConfig.getCategory("init").get("admin.password"));
        assertEquals("custom-"+rand, appConfig.getCategory("custom").get("c1"));
        assertEquals("custom-"+rand, appConfig.getCategory("custom").get("c2"));

        // Install the app
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
        final JsonNode initDatabag = fromJson(FileUtil.toString(chefHandler.getChefDir() + "/data_bags/" + manifest.getScrubbedName() + "/init.json"), JsonNode.class);
        assertEquals(rand, JsonUtil.nodeValue(initDatabag, "test.config1"));

        final JsonNode customDatabag = fromJson(FileUtil.toString(chefHandler.getChefDir() + "/data_bags/" + manifest.getScrubbedName() + "/custom.json"), JsonNode.class);
        assertEquals("custom-"+rand, JsonUtil.nodeValue(customDatabag, "c2"));
    }

    private TaskResult getTaskResult(TaskId taskId) throws Exception {
        long start = System.currentTimeMillis();
        TaskResult result = fromJson(doGet(TASKS_ENDPOINT+"/"+taskId.getUuid()).json, TaskResult.class);
        while (!result.isComplete() && System.currentTimeMillis() - start < TIMEOUT) {
            Thread.sleep(1000);
            final String json = doGet(TASKS_ENDPOINT + "/" + taskId.getUuid()).json;
            result = fromJson(json, TaskResult.class);
        }
        assertTrue(result.isSuccess());
        return result;
    }

}
