package cloudos.resources;

import cloudos.databag.CloudOsDatabag;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;
import rooty.RootyMessage;
import rooty.toots.vendor.VendorSettingDisplayValue;
import rooty.toots.vendor.VendorSettingHandler;
import rooty.toots.vendor.VendorSettingUpdateRequest;

import java.util.*;

import static cloudos.resources.ApiConstants.CONFIGS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationsResourceTest extends ConfigurationTestBase {

    private static final String DOC_TARGET = "Low-level CloudOs Configuration Settings";

    @Test
    public void testListConfigGroups () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "list system configuration groups");
        apiDocs.addNote("Request list of config groups, expect 3: cloudos, email, and system");
        String[] configGroups = fromJson(get(CONFIGS_ENDPOINT).json, String[].class);
        Set<String> groupSet = new HashSet<>();
        groupSet.addAll(Arrays.asList(configGroups));
        assertEquals(3, configGroups.length);
        assertTrue(groupSet.contains("cloudos"));
        assertTrue(groupSet.contains("email"));
        assertTrue(groupSet.contains("system"));
    }

    @Test
    public void testUpdateAuthyKey () throws Exception {

        final String cloudOsConfigPath = CONFIGS_ENDPOINT + "/cloudos";
        final String configSettingPath = cloudOsConfigPath + "/" + AUTHY_SETTING_NAME;
        apiDocs.startRecording(DOC_TARGET, "update a system configuration setting in the cloudos config group");

        Map<String, String> settingsMap;

        apiDocs.addNote("read setting " + AUTHY_SETTING_NAME + " from server, confirm value is empty since it is still the default value");
        settingsMap = getSettings(cloudOsConfigPath);
        assertEquals(VendorSettingHandler.VENDOR_DEFAULT, settingsMap.get(AUTHY_SETTING_NAME));

        // flush rooty messages (otherwise we'll see other messages that have been sent, like NewAccountEvent)
        getRootySender().flush();

        final String newKey = randomAlphanumeric(10);
        apiDocs.addNote("write new value (" + newKey + ") for " + AUTHY_SETTING_NAME);
        RestResponse response = doPost(configSettingPath, newKey);
        assertEquals(200, response.status);
        assertTrue(Boolean.valueOf(response.json));

        // verify the message was correct
        final List<RootyMessage> sent = getRootySender().getSent();
        assertEquals(1, sent.size());
        final VendorSettingUpdateRequest request = (VendorSettingUpdateRequest) sent.get(0);
        assertEquals(AUTHY_SETTING_NAME, request.getSetting().getPath());
        assertEquals(newKey, request.getValue());

        // verify the databag was updated
        final CloudOsDatabag databag = fromJson(FileUtil.toString(cloudosInitDatabag), CloudOsDatabag.class);
        assertEquals(newKey, databag.getAuthy().getUser());

        apiDocs.addNote("re-read setting "+AUTHY_SETTING_NAME+", should see new value");
        settingsMap = getSettings(cloudOsConfigPath);
        assertEquals(newKey, settingsMap.get(AUTHY_SETTING_NAME));
    }

    public Map<String, String> getSettings(String cloudOsConfigPath) throws Exception {

        final RestResponse response = doGet(cloudOsConfigPath);
        final VendorSettingDisplayValue[] values = fromJson(response.json, VendorSettingDisplayValue[].class);

        final Map<String, String> settingsMap = new HashMap<>();
        for (VendorSettingDisplayValue v : values) settingsMap.put(v.getPath(), v.getValue());
        return settingsMap;
    }
}
