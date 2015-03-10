package cloudos.resources.app;

import cloudos.appstore.bundler.BundlerMain;
import cloudos.appstore.bundler.BundlerOptions;
import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.support.AppListing;
import cloudos.model.support.AppDownloadRequest;
import cloudos.resources.ApiClientTestBase;
import cloudos.resources.ApiConstants;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.system.Command;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.util.system.PortPicker;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.util.RestResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.AfterClass;
import rooty.toots.chef.ChefMessage;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.*;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.junit.Assert.*;

@Slf4j
public class AppTestBase extends ApiClientTestBase {

    protected static final String TEST_APP_TARBALL = "test-bundle-@VERSION.tar.gz";

    protected static final String TEST_ICON = "apps/some-icon.png";
    protected static final long TIMEOUT = TimeUnit.SECONDS.toMillis(1000);

    protected static Server testHttpServer;
    protected static int testServerPort;
    protected static File testDocRoot;
    protected static File iconFile;
    protected static String bundleUrl;
    protected static String bundleUrlSha;
    protected static AppManifest appManifest;

    public static void setupTestWebApp(String manifestResourcePath) throws Exception {

        testServerPort = PortPicker.pick();
        testDocRoot = FileUtil.createTempDir(AppInstallTest.class.getName());

        buildAppTarball(manifestResourcePath);

        // Set up jetty server to serve tarball and icon png
        testHttpServer = new Server(testServerPort);

        final ResourceHandler handler = new ResourceHandler();
        handler.setResourceBase(abs(testDocRoot));
        testHttpServer.setHandler(handler);

        testHttpServer.start();
    }

    public static void buildAppTarball(String manifestResourcePath) throws Exception {
        // Write manifest for test app to a temp dir
        final File appTemp = FileUtil.createTempDir("appTemp");
        final File manifestFile = new File(appTemp, AppManifest.CLOUDOS_MANIFEST_JSON);
        final String manifestData = StreamUtil.loadResourceAsString(manifestResourcePath).replace("@@PORT@@", String.valueOf(testServerPort));
        FileUtil.toFile(manifestFile, manifestData);
        appManifest = AppManifest.load(manifestFile);

        File bundleDir = new File(testDocRoot, "scratch");

        // Run the bundler on our test manifest
        final BundlerMain main = new BundlerMain(new String[] {
                BundlerOptions.OPT_MANIFEST, abs(manifestFile),
                BundlerOptions.OPT_OUTPUT_DIR, abs(bundleDir)
        });
        main.runOrDie();

        // Roll the tarball into its place under the doc root
        final String tarballName = TEST_APP_TARBALL.replace("@VERSION", appManifest.getVersion());
        final String tarball = abs(testDocRoot) + "/" + tarballName;
        final CommandLine commandLine = new CommandLine("tar")
                .addArgument("czf")
                .addArgument(tarball)
                .addArgument(".");
        CommandShell.exec(new Command(commandLine).setDir(bundleDir));

        // Save the URL and shasum
        bundleUrl = "http://127.0.0.1:"+testServerPort+"/"+ tarballName;
        bundleUrlSha = ShaUtil.sha256_file(tarball);

        // Copy icon png to doc root
        iconFile = new File(testDocRoot, new File(TEST_ICON).getName());
        FileUtils.copyFile(StreamUtil.loadResourceAsFile(TEST_ICON), iconFile);
    }

    @AfterClass public static void teardownTestApp () throws Exception {
        testHttpServer.stop();
        FileUtils.deleteDirectory(testDocRoot);
    }

    protected AppStoreApiClient getAppStoreClient() {
        return getConfiguration().getAppStoreClient();
    }

    public SearchResults<AppListing> queryAppStore() throws Exception {
        return JsonUtil.fromJson(post(ApiConstants.APPSTORE_ENDPOINT, toJson(ResultPage.DEFAULT_PAGE)).json, SearchResults.jsonType(AppListing.class));
    }

    protected TaskResult downloadApp(String url) throws Exception {
        RestResponse response;
        TaskId taskId;
        TaskResult result;
        final AppDownloadRequest downloadRequest = new AppDownloadRequest()
                .setUrl(url)
                .setToken(randomPassword())
                .setAutoInstall(false);
        apiDocs.addNote("initiate the download request to add the app to app-repository");
        response = doPost(APPS_ENDPOINT + "/download", toJson(downloadRequest));
        assertEquals(200, response.status);
        taskId = fromJson(response.json, TaskId.class);
        result = getTaskResult(taskId);
        return result;
    }

    protected void installApp(AppManifest manifest) throws Exception {
        TaskId taskId;
        TaskResult result;// Install the app
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
    }

    protected TaskResult getTaskResult(TaskId taskId) throws Exception {
        long start = System.currentTimeMillis();
        TaskResult result = null;
        while (System.currentTimeMillis() - start < TIMEOUT) {
            sleep(250, "getTaskResult");
            apiDocs.addNote("check status of task " + taskId.getUuid());
            final String json = doGet(TASKS_ENDPOINT + "/" + taskId.getUuid()).json;
            result = fromJson(json, TaskResult.class);
            if (result.isSuccess()) break;
        }
        log.info("getTaskResult: " + result);
        assertNotNull(result);
        assertTrue(result.isSuccess());
        return result;
    }
}
