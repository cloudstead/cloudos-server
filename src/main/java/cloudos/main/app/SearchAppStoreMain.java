package cloudos.main.app;

import cloudos.main.CloudOsMainBase;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

import static cloudos.resources.ApiConstants.APPSTORE_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class SearchAppStoreMain extends CloudOsMainBase<SearchAppStoreOptions> {

    public static void main (String[] args) throws Exception { main(SearchAppStoreMain.class, args); }

    @Override protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final SearchAppStoreOptions options = getOptions();

        final RestResponse response;
        String uri;
        try {
            if (options.hasPublisher() && options.hasApp()) {
                uri = APPSTORE_ENDPOINT + "/" + options.getPublisher() + "/" + options.getApp();
                if (options.hasVersion()) {
                    uri += "/" + options.getVersion();
                }
                response = api.get(uri);

            } else {
                response = api.post(APPSTORE_ENDPOINT, toJson(options.getQuery()));
            }
            if (response.status != 200) {
                die("Error writing configuration. Response was: " + response);
            } else {
                out(response.json+"\n");
            }
        } catch (Exception e) {
            die("Error writing configuration: " + e, e);
        }

    }
}
