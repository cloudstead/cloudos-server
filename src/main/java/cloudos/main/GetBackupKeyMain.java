package cloudos.main;

import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.SETUP_ENDPOINT;

public class GetBackupKeyMain extends CloudOsMainBase<CloudOsMainOptions> {

    public static void main (String[] args) { main(GetBackupKeyMain.class, args); }

    @Override
    protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final String keyUri = SETUP_ENDPOINT + "/key";
        out("Backup key: "+api.get(keyUri).json);
    }
}
