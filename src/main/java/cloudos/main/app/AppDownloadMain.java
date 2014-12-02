package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import cloudos.model.support.AppDownloadRequest;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class AppDownloadMain extends CloudOsMainBase<AppDownloadOptions> {

    @Override protected AppDownloadOptions initOptions() { return new AppDownloadOptions(); }

    public static void main (String[] args) throws Exception { main(AppDownloadMain.class, args); }

    @Override protected void run() throws Exception {

        final AppDownloadOptions options = getOptions();
        final ApiClientBase api = getApiClient();

        final AppDownloadRequest downloadRequest = new AppDownloadRequest()
                .setUrl(options.getUrl())
                .setToken(options.getToken())
                .setAutoInstall(options.isInstall())
                .setOverwrite(options.isOverwrite());

        final TaskId taskId = fromJson(api.post(APPS_ENDPOINT + "/download", toJson(downloadRequest)).json, TaskId.class);
        TaskResult result = awaitTaskResult(taskId);
        log.info("Downloading completed. TaskResult:\n"+toJson(result)+"\n");
    }

}
