package cloudos.main.account;

import cloudos.main.CloudOsMainBase;
import org.cobbzilla.wizard.api.CrudOperation;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.main.account.CloudOsAccountMainOptions.LONGOPT_NAME;
import static cloudos.main.account.CloudOsAccountMainOptions.OPT_NAME;
import static cloudos.resources.ApiConstants.ACCOUNTS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class CloudOsAccountMain extends CloudOsMainBase<CloudOsAccountMainOptions> {

    public static void main (String[] args) { main(CloudOsAccountMain.class, args); }

    @Override protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final CloudOsAccountMainOptions options = getOptions();
        final String uri = options.hasName() ? ACCOUNTS_ENDPOINT+"/"+options.getName() : ACCOUNTS_ENDPOINT;

        if (options.getOperation() != CrudOperation.read) {
            if (!options.hasName()) throw new IllegalArgumentException("name ("+OPT_NAME+"/"+LONGOPT_NAME+") is required");
        }

        switch (options.getOperation()) {
            case read:
                out(api.get(uri).json);
                break;

            case create:
                out(api.put(uri, toJson(options.getAccountRequest())).json);
                break;

            case update:
                out(api.post(uri, toJson(options.getAccountRequest())).json);
                break;

            case delete:
                out(api.delete(uri).toString());
                break;

            default:
                throw new IllegalArgumentException("unrecognized operation: "+options.getOperation());
        }
    }
}
