package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import org.cobbzilla.wizard.task.TaskId;
import org.cobbzilla.wizard.task.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class AppInstallMain extends CloudOsMainBase<AppInstallOptions> {

    public static void main (String[] args) throws Exception {
        main(AppInstallMain.class, args);
    }

    @Override public void run () throws Exception {

        final ApiClientBase api = getApiClient();
        final AppInstallOptions options = getOptions();

        final String installUri = APPS_ENDPOINT + "/apps/"+options.getAppName()+"/versions/"+options.getVersionName()+"/install"+ options.getForceParam();

        final TaskId taskId = fromJson(api.post(installUri, null).json, TaskId.class);
        TaskResult result = awaitTaskResult(taskId);
        out("Installation completed. TaskResult:\n"+toJson(result)+"\n");
    }


}
