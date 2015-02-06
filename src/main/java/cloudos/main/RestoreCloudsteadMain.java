package cloudos.main;

import cloudos.model.support.RestoreRequest;
import cloudos.resources.setup.SetupSettings;
import cloudos.service.task.TaskId;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.SETUP_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class RestoreCloudsteadMain extends CloudOsMainBase<RestoreCloudsteadOptions> {

    @Override protected RestoreCloudsteadOptions initOptions() { return new RestoreCloudsteadOptions(); }

    public static void main (String[] args) { main(RestoreCloudsteadMain.class, args); }

    @Override
    protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final RestoreCloudsteadOptions options = getOptions();
        final String restoreUri = SETUP_ENDPOINT + "/restore";

        // a bit ugly; find a better way or require the end user to specify it
        final String setupKey = JsonUtil.fromJson(FileUtil.toString("/home/cloudos/.first_time_setup"), SetupSettings.class).getSecret();

        final RestoreRequest restoreRequest = new RestoreRequest()
                .setRestoreKey(options.getKey())
                .setNotifyEmail(options.getNotifyEmail())
                .setSetupKey(setupKey)
                .setInitialPassword(options.getPassword());

        final TaskId taskId = fromJson(api.post(restoreUri, toJson(restoreRequest)).json, TaskId.class);
        out("Restore initiated. TaskId:\n"+toJson(taskId)+"\n");
    }
}
