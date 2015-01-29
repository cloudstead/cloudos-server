package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import cloudos.resources.ApiConstants;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

public class AppManagementMain extends CloudOsMainBase<AppManagementMainOptions> {

    @Override protected AppManagementMainOptions initOptions() { return new AppManagementMainOptions(); }

    public static void main (String[] args) { main(AppManagementMain.class, args); }

    @Override
    protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final RestResponse response = api.get(ApiConstants.APPS_ENDPOINT + "/all");

        if (response.isSuccess()) {
            System.out.println(response.json);
        } else {
            die("Error listing apps: "+response);
        }
    }

}
