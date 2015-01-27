package cloudos.main;

import cloudos.model.support.SslCertificateRequest;
import cloudos.model.support.UnlockRequest;
import cloudos.resources.ApiConstants;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class UnlockMain extends CloudOsMainBase<UnlockMainOptions> {

    @Override protected UnlockMainOptions initOptions() { return new UnlockMainOptions(); }

    public static void main (String[] args) { main(UnlockMain.class, args); }

    @Override protected void run() throws Exception {

        final String uri = ApiConstants.CONFIGS_ENDPOINT + "/unlock";
        final ApiClientBase api = getApiClient();
        final UnlockMainOptions options = getOptions();

        final Boolean allowSsh = getAllowSsh();
        if (!options.isForce() && allowSsh) {
            showHelpAndExit("Cloudstead is already unlocked. Use "+UnlockMainOptions.OPT_FORCE+"/"+UnlockMainOptions.LONGOPT_FORCE+" to forcibly re-unlock the system with a new default https certificate and Authy user");
        }

        if (allowSsh) log.warn("Re-unlocking cloudstead...");

        final SslCertificateRequest request = new SslCertificateRequest()
                .setPem(FileUtil.toString(options.getPem()))
                .setKey(FileUtil.toString(options.getKey()))
                .setName("ssl-https");

        final Map<String, String> settings = new HashMap<>();
        settings.put("init/authy.user", options.getAuthy());
        final UnlockRequest unlockRequest = new UnlockRequest().setCert(request).setSettings(settings);

        final RestResponse response = api.doPut(uri, toJson(unlockRequest));
        if (!response.isSuccess()) die("Error unlocking cloudstead: "+response);

        if (!getAllowSsh()) die("Unlocking seems to have succeeded, but ALLOW_SSH is still false.");

        System.out.println("Successfully unlocked cloudstead");
    }
}
