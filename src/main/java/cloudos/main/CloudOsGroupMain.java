package cloudos.main;

import cloudos.model.support.AccountGroupRequest;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.main.CloudOsGroupMainOptions.*;
import static cloudos.resources.ApiConstants.GROUPS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class CloudOsGroupMain extends CloudOsMainBase<CloudOsGroupMainOptions> {

    @Override protected CloudOsGroupMainOptions initOptions() { return new CloudOsGroupMainOptions(); }

    public static void main (String[] args) {
        main(CloudOsGroupMain.class, args);
    }

    @Override
    protected void run() throws Exception {

        final ApiClientBase api = getApiClient();
        final CloudOsGroupMainOptions options = getOptions();
        final String uri = options.hasName() ? GROUPS_ENDPOINT+"/"+options.getName() : GROUPS_ENDPOINT;
        final AccountGroupRequest request;

        if (options.getOperation() != CloudOsGroupOperation.view) {
            if (!options.hasName()) throw new IllegalArgumentException("name ("+OPT_NAME+"/"+LONGOPT_NAME+") is required");
        }

        switch (options.getOperation()) {
            case view:
                out(api.get(uri).json);
                break;

            case create:
                request = new AccountGroupRequest()
                        .setName(options.getName())
                        .setInfo(options.getInfo())
                        .setRecipients(options.getRecipients());
                out(api.put(uri, toJson(request)).json);
                break;

            case update:
                request = new AccountGroupRequest()
                        .setName(options.getName())
                        .setInfo(options.getInfo())
                        .setRecipients(options.getRecipients());
                out(api.post(uri, toJson(request)).json);
                break;

            case delete:
                out(api.delete(uri).toString());
                break;

            default:
                throw new IllegalArgumentException("unrecognized operation: "+options.getOperation());
        }
    }
}
