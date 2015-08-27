package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.APPS_ENDPOINT;
import static cloudos.resources.ApiConstants.EP_REFRESH;

public class RefreshAppsMain extends CloudOsMainBase<RefreshAppsMainOptions>{

    public static void main (String[] args) { main(RefreshAppsMain.class, args); }

    @Override protected void run() throws Exception {

        final RefreshAppsMainOptions options = getOptions();
        final ApiClientBase api = getApiClient();
        String uri = APPS_ENDPOINT + EP_REFRESH;

        if (options.hasRefreshKey()) {
            uri += "?refreshKey=" + options.getRefreshKey();
        } else {
            login();
        }

        out(api.get(uri).toString());
    }
}
