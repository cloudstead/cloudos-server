package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import cloudos.resources.ApiConstants;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

public class AppManagementMain extends CloudOsMainBase<AppManagementMainOptions> {

    public static void main (String[] args) { main(AppManagementMain.class, args); }

    @Override
    protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final RestResponse response = api.get(ApiConstants.APPS_ENDPOINT + "/all");

        if (response.isSuccess()) {
            out(response.json);
        } else {
            die("Error listing apps: "+response);
        }
    }

}
