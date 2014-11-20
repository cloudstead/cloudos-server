package cloudos.resources;

import cloudos.databag.CloudOsDatabag;
import cloudos.databag.EmailDatabag;
import rooty.toots.vendor.VendorDatabagSetting;
import rooty.toots.vendor.VendorDatabag;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.security.ShaUtil;
import org.junit.Before;

import java.io.File;

public class ConfigurationTestBase extends ApiClientTestBase {

    protected File cloudosInitDatabag;
    protected File emailDatabag;

    public static final String AUTHY_DEFAULT_VALUE = "authy-key-goes-here";
    public static final String AUTHY_SETTING_NAME = "authy.user";
    public static final String AUTHY_SETTING_PATH = "init/" + AUTHY_SETTING_NAME;

    public static final String[][] EMAIL_SETTINGS = {
            { "smtp_relay.username", "sg-user-foo" },
            { "smtp_relay.password", "sg-pass-bar" },
            { "smtp_relay.host",     "example.com" },
            { "smtp_relay.port",     "587" },
    };
    public static final String[][] CLOUDOS_SETTINGS = {
            { AUTHY_SETTING_NAME,  AUTHY_DEFAULT_VALUE},
            { "aws_access_key",    "aws-something" },
            { "aws_secret_key",    "aws-secret-something" },
            { "aws_iam_user",      "iam-a-robot" },
            { "s3_bucket",         "some-bucket" },
    };

    public String[][] getVendorSettings () { return CLOUDOS_SETTINGS; }

    protected Object initDatabag(Object databag) {
        final Class<?> databagClass = databag.getClass();
        if (databagClass == CloudOsDatabag.class) {
            addSettings(databag, CLOUDOS_SETTINGS);
        } else if (databagClass == EmailDatabag.class) {
            addSettings(databag, EMAIL_SETTINGS);
        } else {
            throw new IllegalArgumentException("no id in databag: "+databag);
        }
        return databag;
    }

    private void addSettings(Object databag, String[][] settings) {
        final VendorDatabag vendor = (VendorDatabag) ReflectionUtil.get(databag, "vendor");
        for (String[] setting : settings) {
            if (setting[0].contains(".port")) {
                ReflectionUtil.set(databag, setting[0], Integer.parseInt(setting[1]));
            } else {
                ReflectionUtil.set(databag, setting[0], setting[1]);
            }
            boolean blockSsh = setting[0].equals(AUTHY_SETTING_NAME);
            vendor.addSetting(new VendorDatabagSetting(setting[0], ShaUtil.sha256_hex(setting[1]), blockSsh));
        }
    }

    @Before
    public void initCloudOsConfig () throws Exception {

        // write databag with test data
        final CloudOsDatabag databag = (CloudOsDatabag) initDatabag(new CloudOsDatabag());
        cloudosInitDatabag = new File(vendorSettingHandler.getChefDir()+"/data_bags/cloudos/init.json");
        cloudosInitDatabag.getParentFile().mkdirs();
        FileUtil.toFile(cloudosInitDatabag, JsonUtil.toJson(databag));

        final EmailDatabag emailbag = (EmailDatabag) initDatabag(new EmailDatabag());
        emailDatabag = new File(vendorSettingHandler.getChefDir()+"/data_bags/email/init.json");
        emailDatabag.getParentFile().mkdirs();
        FileUtil.toFile(emailDatabag, JsonUtil.toJson(emailbag));
    }

}
