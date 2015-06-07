package cloudos.resources.app;

import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.support.AppListing;
import cloudos.model.support.AppDownloadRequest;
import cloudos.resources.ApiClientTestBase;
import cloudos.resources.ApiConstants;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.util.RestResponse;
import rooty.toots.chef.ChefMessage;

import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.junit.Assert.*;

@Slf4j
public class AppTestBase extends ApiClientTestBase {

    protected static final long TIMEOUT = TimeUnit.SECONDS.toMillis(1000);

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
        assertEquals(manifest.getScrubbedName(), chefMessage.getCookbook());
    }

    protected TaskResult getTaskResult(TaskId taskId) throws Exception {
        long start = System.currentTimeMillis();
        TaskResult result = null;
        while (System.currentTimeMillis() - start < TIMEOUT) {
            sleep(1000, "getTaskResult");
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
