package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import cloudos.model.support.AppUninstallRequest;
import cloudos.resources.ApiConstants;
import cloudos.service.task.TaskId;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

public class AppUninstallMain extends CloudOsMainBase<AppUninstallMainOptions> {

    public static void main (String[] args) { main(AppUninstallMain.class, args); }

    @Override protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final AppUninstallMainOptions options = getOptions();
        final String path = ApiConstants.APPS_ENDPOINT + "/apps/" + options.getAppName() + "/versions/" + options.getAppVersion() + "/uninstall";

        final RestResponse post = api.post(path, JsonUtil.toJson(new AppUninstallRequest(options.getMode())));

        final TaskId taskId = JsonUtil.fromJson(post.json, TaskId.class);
        awaitTaskResult(taskId);
    }
}
