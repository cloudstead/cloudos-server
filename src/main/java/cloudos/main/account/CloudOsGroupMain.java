package cloudos.main.account;

import cloudos.main.CloudOsMainBase;
import org.cobbzilla.wizard.api.CrudOperation;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.main.account.CloudOsGroupMainOptions.LONGOPT_NAME;
import static cloudos.main.account.CloudOsGroupMainOptions.OPT_NAME;
import static cloudos.resources.ApiConstants.GROUPS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class CloudOsGroupMain extends CloudOsMainBase<CloudOsGroupMainOptions> {

    @Override protected CloudOsGroupMainOptions initOptions() { return new CloudOsGroupMainOptions(); }

    public static void main (String[] args) { main(CloudOsGroupMain.class, args); }

    @Override protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final CloudOsGroupMainOptions options = getOptions();
        final String uri = options.hasName() ? GROUPS_ENDPOINT+"/"+options.getName() : GROUPS_ENDPOINT;

        if (options.getOperation() != CrudOperation.read) {
            if (!options.hasName()) throw new IllegalArgumentException("name ("+OPT_NAME+"/"+LONGOPT_NAME+") is required");
        }

        switch (options.getOperation()) {
            case read:
                out(api.get(uri).json);
                break;

            case create:
                out(api.put(uri, toJson(options.getGroupRequest())).json);
                break;

            case update:
                out(api.post(uri, toJson(options.getGroupRequest())).json);
                break;

            case delete:
                out(api.delete(uri).toString());
                break;

            default:
                throw new IllegalArgumentException("unrecognized operation: "+options.getOperation());
        }
    }
}
