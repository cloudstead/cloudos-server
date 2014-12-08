package cloudos.resources;

import cloudos.appstore.test.MockAppStoreApiClient;
import cloudos.dao.SslCertificateDAO;
import cloudos.dns.service.mock.MockDnsManager;
import cloudos.model.Account;
import cloudos.model.SslCertificate;
import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountGroupView;
import cloudos.model.support.AccountRequest;
import cloudos.model.support.SetupRequest;
import cloudos.resources.setup.MockSetupSettingsSource;
import cloudos.server.CloudOsConfiguration;
import cloudos.server.CloudOsServer;
import cloudos.service.MockKerberosService;
import cloudos.service.MockRootySender;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.io.Files;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailService;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.util.time.ImprovedTimezone;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;
import org.junit.Before;
import rooty.toots.chef.ChefHandler;
import rooty.toots.chef.ChefSolo;
import rooty.toots.service.ServiceKeyHandler;
import rooty.toots.ssl.SslCertHandler;
import rooty.toots.system.SystemSetTimezoneMessage;
import rooty.toots.vendor.VendorSettingHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cloudos.resources.ApiConstants.ACCOUNTS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.wizardtest.RandomUtil.randomEmail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class ApiClientTestBase extends ApiDocsResourceIT<CloudOsConfiguration, CloudOsServer> {

    public static final String DEFAULT_KEY_SHA = "1844d332ccb478a82eb038e947988b1b1e5b7882ddda85456efbc89bca327e97";
    public static final String DEFAULT_KEY_MD5 = "23a5bcd716f54cc819a7367e64fe70e9";
    public static final String DEFAULT_PEM_SHA = "761f5e4128089695d51600c36e6b96438828e0ee7c76d7a15da2c13516832417";
    public static final String DEFAULT_PEM_MD5 = "e367ebcdec3792a33c0005e8b8098040";

    private MockDnsManager dnsManager = new MockDnsManager();
    private MockAppStoreApiClient appStoreClient = new MockAppStoreApiClient();

    public boolean shouldCacheServer () { return false; }

    public void flushTokens() { tokenStack.clear(); setToken(null); }

    public static AccountRequest newAccountRequest(String accountName) {
        return newAccountRequest(accountName, null, false);
    }

    public static AccountRequest newAccountRequest(String accountName, String password, boolean isAdmin) {
        // pop-quiz, java stupidity 101: why is this cast necessary?
        return (AccountRequest) new AccountRequest()
                .setPassword(password)
                .setAccountName(accountName)
                .setMobilePhone(randomNumeric(10))
                .setMobilePhoneCountryCode(1)
                .setEmail(randomEmail())
                .setFirstName(randomAlphanumeric(10))
                .setLastName(randomAlphanumeric(10))
                .setAdmin(isAdmin);
    }

    protected String getTestConfig() { return "cloudos-config-test.yml"; }

    @Before public void setupMocks() throws Exception {}

    public MockTemplatedMailService getTemplatedMailService() { return getBean(MockTemplatedMailService.class); }
    public MockTemplatedMailSender getTemplatedMailSender() { return (MockTemplatedMailSender) getTemplatedMailService().getMailSender(); }

    public MockKerberosService getKerberos() { return getBean(MockKerberosService.class); }

    public CloudOsConfiguration getConfiguration() { return (CloudOsConfiguration) server.getConfiguration(); }
    public MockRootySender getRootySender() { return (MockRootySender) getConfiguration().getRooty().getSender(); }
    public MockDnsManager getDnsManager() { return ((MockDnsManager) getConfiguration().getDnsManager()); }

    public static final String TEST_ENV_FILE = ".cloudos-test.env";
    @Getter private final Map<String, String> serverEnvironment = initServerEnvironment();
    protected Map<String, String> initServerEnvironment () {
        final Map<String, String> env = CommandShell.loadShellExportsOrDie(TEST_ENV_FILE);
        env.put("CLOUD_STORAGE_DATA_KEY", randomAlphanumeric(20));
        return env;
    }

    @Override
    protected List<ConfigurationSource> getConfigurations() {
        final List<ConfigurationSource> sources = new ArrayList<>();
        sources.add(new StreamConfigurationSource(getClass().getClassLoader().getResourceAsStream(getTestConfig())));
        return sources;
    }

    @Override protected Class<CloudOsServer> getRestServerClass() { return CloudOsServer.class; }

    public static final String TEST_KEY = "ssl-https.key";
    public static final String TEST_PEM = "ssl-https.pem";
    public static final String DUMMY_KEY = "dummy.key";
    public static final String DUMMY_PEM = "dummy.pem";

    public String getDummyKey() { return StreamUtil.loadResourceAsStringOrDie("ssl/" + DUMMY_KEY); }
    public String getDummyPem() { return StreamUtil.loadResourceAsStringOrDie("ssl/" + DUMMY_PEM); }
    public String getTestKey() { return StreamUtil.loadResourceAsStringOrDie("ssl/" + TEST_KEY); }
    public String getTestPem() { return StreamUtil.loadResourceAsStringOrDie("ssl/" + TEST_PEM); }

    protected File sslKeysDir;
    protected ServiceKeyHandler serviceKeyHandler;
    protected SslCertHandler certHandler;
    protected VendorSettingHandler vendorSettingHandler;
    protected ChefHandler chefHandler;
    protected File chefHome;
    protected File appRepository;

    @Override public void beforeStart(RestServer<CloudOsConfiguration> server) {
        try { _beforeStart(); } catch (Exception e) {
            throw new IllegalStateException("Error in beforeStart: "+e, e);
        }
        super.beforeStart(server);
    }

    protected void _beforeStart() throws Exception {
        // Write default ssl cert to disk and to DB
        sslKeysDir = Files.createTempDir();
        final String sslKeysPath = sslKeysDir.getAbsolutePath();

        final File pemFile = new File(sslKeysDir, TEST_PEM);
        final File keyFile = new File(sslKeysDir, TEST_KEY);

        FileUtil.toFile(pemFile, getTestPem());
        FileUtil.toFile(keyFile, getTestKey());

        // Create dummy cacerts file. DB record is created later (see below)
        final File cacertsFile = File.createTempFile("cacerts", ".keystore");
        final String keystorePassword = RandomStringUtils.randomAlphanumeric(10);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileOutputStream out = new FileOutputStream(cacertsFile)) {
            ks.load(null, keystorePassword.toCharArray());
            ks.store(out, keystorePassword.toCharArray());
        }

        // setup handler for certificate requests (ssl keys)
        certHandler = new SslCertHandler();
        certHandler.setPemPath(sslKeysPath);
        certHandler.setKeyPath(sslKeysPath);
        certHandler.setCacertsFile(cacertsFile.getAbsolutePath());
        certHandler.setKeystorePassword(keystorePassword);

        // setup handler for service key requests (ssh keys)
        serviceKeyHandler = new ServiceKeyHandler() {
            @Override public String getChefUserHome() { return chefHome.getAbsolutePath(); }
            @Override public String getChefDir() { return chefHome.getAbsolutePath(); }
            @Override protected String initChefUser() { return "nobody"; }
        };
        serviceKeyHandler.setSslKeysDir(sslKeysPath);
        serviceKeyHandler.setDefaultSslKeySha(ShaUtil.sha256_file(keyFile));
        serviceKeyHandler.setServiceDir(Files.createTempDir().getAbsolutePath());

        // the vendor endpoint is called when a test requests a rootyService key with a recipient of VENDOR
        final String vendorEndpoint = server.getClientUri() + MockServiceRequestsResource.ENDPOINT;
        serviceKeyHandler.setServiceKeyEndpoint(vendorEndpoint);

        // the settings handler
        chefHome = Files.createTempDir();
        vendorSettingHandler = new VendorSettingHandler() {
            @Override public String getChefUserHome() { return chefHome.getAbsolutePath(); }
            @Override public String getChefDir() { return chefHome.getAbsolutePath(); }
            @Override protected String initChefUser() { return "nobody"; }
        };

        // the chef handler (for AppInstallTest)
        chefHandler = new ChefHandler() {
            @Override public String getChefUserHome() { return chefHome.getAbsolutePath(); }
            @Override public String getChefDir() { return chefHome.getAbsolutePath(); }
            @Override protected String initChefUser() { return "nobody"; }
            @Override protected void runChefSolo() throws Exception { /* noop */ }
        };
        // write a simple solo.json file
        final ChefSolo chefSolo = new ChefSolo();
        chefSolo.setRun_list(new String[] {"recipe[test]", "recipe[test-foo]"});
        FileUtil.toFile(new File(chefHome, "solo.json"), JsonUtil.toJson(chefSolo));

        // register mocks with rooty
        final CloudOsConfiguration configuration = (CloudOsConfiguration) serverHarness.getConfiguration();
        configuration.setRooty(new MockRootyConfiguration());
        configuration.getRooty().addHandler(serviceKeyHandler);
        configuration.getRooty().addHandler(vendorSettingHandler);
        configuration.getRooty().addHandler(certHandler);
        configuration.getRooty().addHandler(chefHandler);

        // mock app store and DNS manager
        configuration.setAppStoreClient(appStoreClient);
        configuration.setDnsManager(dnsManager);

        // use scratch dir for app repository, set rooty group to null (skip chgrp)
        appRepository = Files.createTempDir();
        configuration.setAppRepository(appRepository);
        configuration.setRootyGroup(null);
    }

    @Before
    public void createDefaultCertRecord () throws Exception {
        final SslCertificateDAO certDAO = getBean(SslCertificateDAO.class);
        certDAO.create((SslCertificate) new SslCertificate()
                .setCommonName("*.cloudstead.io")
                .setDescription("cloudstead.io wildcard certificate")
                .setKeySha(DEFAULT_KEY_SHA)
                .setKeyMd5(DEFAULT_KEY_MD5)
                .setPemSha(DEFAULT_PEM_SHA)
                .setPemMd5(DEFAULT_PEM_MD5)
                .setName(ApiConstants.DEFAULT_CERT_NAME));
    }

    protected String adminToken;
    protected Account admin;

    @Override protected String getTokenHeader() { return ApiConstants.H_API_KEY; }

    protected boolean skipAdminCreation() { return false; }

    @Before
    public void createAdminUser () throws Exception {

        if (skipAdminCreation()) return;

        // Peek into Spring context to find out what the setup secrets are in the mock
        final MockSetupSettingsSource setupSource  = server.getApplicationContext().getBean(MockSetupSettingsSource.class);

        // Build first-time setup request
        final int timezoneId = 4;
        final String accountName = randomAlphanumeric(10);
        final String password = randomAlphanumeric(10);
        final SetupRequest request = (SetupRequest) new SetupRequest()
                .setSetupKey(setupSource.getMockSettings().getSecret())
                .setSystemTimeZone(timezoneId)
                .setInitialPassword(setupSource.getPassword())
                .setPassword(password)
                .setAccountName(accountName)
                .setMobilePhone(randomNumeric(10))
                .setMobilePhoneCountryCode(1)
                .setEmail(randomEmail())
                .setFirstName(randomAlphanumeric(10))
                .setLastName(randomAlphanumeric(10))
                .setAdmin(true);

        // Do first-time setup, create cloudOs admin
        final RestResponse response = post(ApiConstants.SETUP_ENDPOINT, toJson(request));
        final AuthResponse authResponse = fromJson(response.json, CloudOsAuthResponse.class);

        // ensure kerberos got the message
        assertEquals(request.getPassword(), getKerberos().getPassword(request.getAccountName()));

        // ensure system timezone setter was done
        final SystemSetTimezoneMessage tzMessage = getRootySender().first(SystemSetTimezoneMessage.class);
        assertNotNull(tzMessage);
        assertEquals(ImprovedTimezone.getTimeZoneById(request.getSystemTimeZone()).getLinuxName(), tzMessage.getTimezone());

        adminToken = authResponse.getSessionId();
        admin = (Account) authResponse.getAccount();
        setToken(adminToken);
    }

    public RestResponse login(LoginRequest loginRequest) throws Exception {
        apiDocs.addNote("login: " + loginRequest);
        final RestResponse response = doPost(ACCOUNTS_ENDPOINT, toJson(loginRequest));
        if (response.status == 200) {
            final AuthResponse authResponse = fromJson(response.json, CloudOsAuthResponse.class);
            if (authResponse.hasSessionId()) pushToken(authResponse.getSessionId());
        }
        return response;
    }

    public void suspend(AccountRequest request) throws Exception {
        apiDocs.addNote("suspending account: "+request.getAccountName());
        request.setSuspended(true);
        update(request);
    }

    public void update(AccountRequest request) throws Exception {
        pushToken(adminToken);
        RestResponse response = post(ACCOUNTS_ENDPOINT + "/" + request.getAccountName(), toJson(request));
        assertEquals(200, response.status);
        popToken();
    }

    public SearchResults<Account> searchAccounts(ResultPage page) throws Exception {
        return search(page, "accounts", Account.searchResultType);
    }

    public SearchResults<AccountGroupView> searchAccountGroups(ResultPage page) throws Exception {
        return search(page, "groups", AccountGroupView.searchResultType);
    }

    public <T> SearchResults<T> search(ResultPage page, String type, JavaType resultType) throws Exception {
        apiDocs.addNote("search " + type + " with query: " + page);
        final RestResponse response = doPost(ApiConstants.SEARCH_ENDPOINT + "/" + type, toJson(page));
        return JsonUtil.PUBLIC_MAPPER.readValue(response.json, resultType);
    }

    public String downloadAccounts(ResultPage page) throws Exception {
        return download(page, "accounts", Account.searchResultType);
    }

    public String downloadAccountGroups(ResultPage page) throws Exception {
        return download(page, "groups", AccountGroupView.searchResultType);
    }

    private String download(ResultPage page, String type, JavaType resultType) throws Exception {
        apiDocs.addNote("downloading CSV of " + type + " with query: " + page);
        final RestResponse response = doGet(ApiConstants.SEARCH_ENDPOINT + "/" + type + "/download.csv?page=" + urlEncode(toJson(page)));
        return response.json;
    }
}
