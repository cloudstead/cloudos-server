package cloudos.main;

import cloudos.resources.ApiConstants;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import rooty.toots.service.ServiceKeyRequest;

import static cloudos.main.ServiceKeyMainOptions.LONGOPT_NAME;
import static cloudos.main.ServiceKeyMainOptions.OPT_NAME;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class ServiceKeyMain extends CloudOsMainBase<ServiceKeyMainOptions> {

    @Override protected ServiceKeyMainOptions initOptions() { return new ServiceKeyMainOptions(); }

    public static void main (String[] args) { main(ServiceKeyMain.class, args); }

    @Override protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final ServiceKeyMainOptions options = getOptions();
        String uri = ApiConstants.SERVICE_KEYS_ENDPOINT;

        final ServiceKeyRequest.Operation operation = options.getOperation();
        if (operation == null) {
            System.out.println("Found valet keys: ");
            System.out.println(api.get(uri).json);
            return;
        }

        if (!options.hasName()) {
            showHelpAndExit("name argument ("+OPT_NAME+"/"+LONGOPT_NAME+") is required for "+operation+" operation");
        }

        final RestResponse response;
        final String keyName = options.getName();
        uri += "/"+ keyName;

        switch (operation) {
            case GENERATE:
                final ServiceKeyRequest request = new ServiceKeyRequest(keyName, operation, options.getRecipient());
                response = api.doPost(uri, toJson(request));
                if (response.isSuccess()) {
                    if (options.getRecipient() == ServiceKeyRequest.Recipient.VENDOR) {
                        out("Successfully created valet key "+keyName+" and sent to vendor");
                    } else {
                        out("Successfully created valet key "+keyName+":\n" + response.json);
                    }
                } else {
                    die("Error creating valet key:\n"+response);
                }
                break;

            case DESTROY:
                response = api.doDelete(uri);
                if (response.isSuccess()) {
                    out("Successfully removed valet key "+ keyName);
                } else {
                    die("Error removing valet key "+ keyName +":\n"+response);
                }
                break;

            default:
                showHelpAndExit("invalid operation: "+ operation);
        }
    }
}
