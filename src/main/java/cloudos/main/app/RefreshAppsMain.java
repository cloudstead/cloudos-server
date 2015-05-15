package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import cloudos.resources.ApiConstants;
import org.cobbzilla.wizard.client.ApiClientBase;

public class RefreshAppsMain extends CloudOsMainBase<RefreshAppsMainOptions>{

    public static void main (String[] args) { main(RefreshAppsMain.class, args); }

    @Override protected void run() throws Exception {

        final RefreshAppsMainOptions options = getOptions();
        final ApiClientBase api = getApiClient();
        String uri = ApiConstants.APPS_ENDPOINT+"/refresh";

        if (options.hasRefreshKey()) {
            uri += "?refreshKey=" + options.getRefreshKey();
        } else {
            login();
        }

        out(api.get(uri).toString());
    }
}
