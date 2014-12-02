package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;

@Slf4j
public class AppConfigMain extends CloudOsMainBase<AppConfigOptions> {

    @Override protected AppConfigOptions initOptions() { return new AppConfigOptions(); }

    public static void main (String[] args) throws Exception { main(AppConfigMain.class, args); }

    @Override protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final AppConfigOptions options = getOptions();
        final String configUri = APPS_ENDPOINT + "/apps/"+options.getAppName()+"/versions/"+options.getAppVersion()+"/config";

        if (options.hasConfig()) {
            // write config
            final RestResponse response;
            try {
                response = api.post(configUri, options.getConfigJson());
                if (response.status != 200) {
                    log.error("Error writing configuration. Response was: " + response);
                }
            } catch (Exception e) {
                log.error("Error writing configuration: " + e, e);
            }
        }

        // read config
        try {
            final String configJson = api.get(configUri).json;
            log.info("Current configuration:\n" + configJson);

        } catch (Exception e) {
            log.error("Error reading configuration: "+e, e);
        }
    }
}
