package cloudos.resources;

import cloudos.model.support.SslCertificateRequest;
import cloudos.model.support.UnlockRequest;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;
import rooty.toots.service.ServiceKeyHandler;
import rooty.toots.service.ServiceKeyRequest;
import rooty.toots.service.ServiceKeyVendorMessage;
import rooty.toots.vendor.VendorSettingDisplayValue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;
import static rooty.toots.service.ServiceKeyRequest.Operation.GENERATE;
import static rooty.toots.service.ServiceKeyRequest.Recipient.CUSTOMER;
import static rooty.toots.service.ServiceKeyRequest.Recipient.VENDOR;

public class ServiceKeyTest extends ConfigurationTestBase {

    public static final String DOC_TARGET = "Service Key Generation";

    @Test
    public void testGenerateKey () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "generate a service key, overwrite default SSL cert, generate a key visible to end-user");

        // Check "allowssh" config value -- should be false
        apiDocs.addNote("check 'allowssh' config setting, should be false");
        assertFalse(getAllowSsh());

        final String keyName = randomAlphanumeric(10).toLowerCase();

        final ServiceKeyRequest customerKeyRequest = new ServiceKeyRequest()
                .setName(keyName)
                .setOperation(GENERATE)
                .setRecipient(CUSTOMER);

        final ServiceKeyRequest vendorKeyRequest = new ServiceKeyRequest()
                .setName(keyName)
                .setOperation(GENERATE)
                .setRecipient(VENDOR);

        // Generate key, request private key -- this will fail.
        apiDocs.addNote("request a private service key. this fails because the vendor SSL cert is still installed");
        RestResponse response = doPost(serviceKeyUri(keyName), toJson(customerKeyRequest));
        assertEquals(HttpStatusCodes.SERVER_ERROR, response.status);

        // Generate key, request private key to be securely emailed to cloudstead.io
        apiDocs.addNote("request a service key to be sent to vendor, this is allowed");
        response = doPost(serviceKeyUri(keyName), toJson(vendorKeyRequest));
        assertEquals(HttpStatusCodes.OK, response.status);
        assertTrue(StringUtil.empty(response.json));

        // Verify private key sent to vendor
        final MockServiceRequestsResource vendor = server.getApplicationContext().getBean(MockServiceRequestsResource.class);
        assertEquals(1, vendor.getServiceRequests().size());
        assertEquals(readPrivateKeyFromDisk(keyName), vendor.getServiceRequests().get(0).getKey());

        // Unlock the cloudstead: overwrite default SSL cert and all default settings
        final SslCertificateRequest request = new SslCertificateRequest()
                .setPem(getDummyPem())
                .setKey(getDummyKey())
                .setName("ssl-https");

        final Map<String, String> settings = new HashMap<>();
        settings.put(ConfigurationsResourceTest.AUTHY_SETTING_NAME, randomAlphanumeric(10));
        final UnlockRequest unlockRequest = new UnlockRequest().setCert(request).setSettings(settings);

        apiDocs.addNote("unlock the cloudstead: overwrite default ssl cert and update all required vendor settings");
        response = doPut(ApiConstants.CONFIGS_ENDPOINT+"/unlock", toJson(unlockRequest));
        assertEquals(HttpStatusCodes.OK, response.status);

        // Check "allowssh" config value -- should now be true
        apiDocs.addNote("check 'allowssh' config setting, should now be true");
        assertTrue(getAllowSsh());

        // Generate key, request private key -- this will succeed
        final String keyName2 = keyName + "_2";
        customerKeyRequest.setUuid(null);
        customerKeyRequest.setName(keyName2);
        apiDocs.addNote("request a private service key, this will now succeed since the default ssl cert has been replaced");
        response = doPost(serviceKeyUri(keyName2), toJson(customerKeyRequest));
        assertEquals(HttpStatusCodes.OK, response.status);

        // assert response contains private key
        assertEquals(readPrivateKeyFromDisk(keyName2), fromJson(response.json, ServiceKeyVendorMessage.class).getKey());
    }

    public String readPrivateKeyFromDisk(String keyName) {
        return FileUtil.toStringOrDie(new File(serviceKeyHandler.getKeyDir(), ServiceKeyHandler.keyname(keyName)));
    }

    public String serviceKeyUri(String keyName) {
        return ApiConstants.SERVICE_KEYS_ENDPOINT + "/" + keyName;
    }

    public Boolean getAllowSsh() throws Exception {
        final RestResponse response = doGet(ApiConstants.CONFIGS_ENDPOINT + "/system/allowssh");
        return Boolean.valueOf(fromJson(response.json, VendorSettingDisplayValue.class).getValue());
    }

}