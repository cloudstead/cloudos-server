package cloudos.resources;

import org.apache.commons.lang3.RandomStringUtils;

public class ApiConstants {

    public static final String H_API_KEY = "x-cloudos-api-key";

    public static final String AUTH_ENDPOINT = "/auth";
    public static final String SETUP_ENDPOINT = "/setup";
    public static final String SESSIONS_ENDPOINT = "/sessions";
    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String SEARCH_ENDPOINT = "/search";
    public static final String GROUPS_ENDPOINT = "/groups";
    public static final String DNS_ENDPOINT = "/dns";

    public static final String EMAIL_ENDPOINT = "/email";

    public static final String EP_DOMAINS = "/domains";
    public static final String EMAIL_DOMAINS_ENDPOINT = EMAIL_ENDPOINT + EP_DOMAINS;

    public static final String SECURITY_ENDPOINT = "/security";
    public static final String EP_CERTS = "/certs";
    public static final String CERTS_ENDPOINT = SECURITY_ENDPOINT + EP_CERTS;

    public static final String EP_SERVICE_KEYS = "/service_keys";
    public static final String SERVICE_KEYS_ENDPOINT = SECURITY_ENDPOINT + EP_SERVICE_KEYS;

    public static final String CONFIGS_ENDPOINT = "/configs";
    public static final String APP_ADAPTER_ENDPOINT = "/app";
    public static final String APPS_ENDPOINT = "/apps";
    public static final String TASKS_ENDPOINT = "/tasks";

    public static final String APPSTORE_ENDPOINT = "/appstore";
    public static final String DEFAULT_CERT_NAME = "ssl-https";

    public static String randomPassword() {
        return RandomStringUtils.randomAlphabetic(2).toLowerCase() + RandomStringUtils.randomNumeric(2)
                + RandomStringUtils.randomAlphabetic(2).toLowerCase() + RandomStringUtils.randomNumeric(2);
    }
}
