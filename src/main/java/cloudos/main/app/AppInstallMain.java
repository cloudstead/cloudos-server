package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class AppInstallMain extends CloudOsMainBase<AppInstallOptions> {

    @Override protected AppInstallOptions initOptions() { return new AppInstallOptions(); }

    public static void main (String[] args) throws Exception {
        main(AppInstallMain.class, args);
    }

    @Override public void run () throws Exception {

        final ApiClientBase api = getApiClient();
        final AppInstallOptions options = getOptions();
        final String installUri = APPS_ENDPOINT + "/apps/"+options.getAppName()+"/versions/"+options.getAppVersion()+"/install"+ options.getForceParam();

        final TaskId taskId = fromJson(api.post(installUri, null).json, TaskId.class);
        TaskResult result = awaitTaskResult(taskId);
        log.info("Installation completed. TaskResult:\n"+toJson(result)+"\n");
    }


}
