package cloudos.service;

import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.ConfigurableAppRuntime;
import cloudos.appstore.model.app.AppAuthConfig;
import cloudos.model.app_impl.RoundcubeAppRuntime;
import cloudos.server.CloudOsConfiguration;
import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.CookieJar;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.util.BufferedResponse;
import org.cobbzilla.wizard.util.ProxyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service @Slf4j
public class InstalledAppLoader {

    public static final String APP_EMAIL = "email";
    public static final String APP_CALENDAR = "calendar";
    public static final String APP_FILES = "files";

    public static final AppRuntimeDetails ROUNDCUBE_DETAILS = new AppRuntimeDetails(APP_EMAIL, "/roundcube/", null);
    public static final AppRuntimeDetails ROUNDCUBE_CAL_DETAILS = new AppRuntimeDetails(APP_CALENDAR, "/roundcube/?_task=calendar", null);
    public static final AppRuntimeDetails OWNCLOUD_DETAILS = new AppRuntimeDetails(APP_FILES, "/owncloud/", null);

    public static final RoundcubeAppRuntime ROUNDCUBE = new RoundcubeAppRuntime(ROUNDCUBE_DETAILS);
    public static final RoundcubeAppRuntime ROUNDCUBE_CAL = new RoundcubeAppRuntime(ROUNDCUBE_CAL_DETAILS);
//    public static final OwncloudAppRuntime OWNCLOUD = new OwncloudAppRuntime(OWNCLOUD_DETAILS);
    public static final ConfigurableAppRuntime OWNCLOUD = (ConfigurableAppRuntime) new ConfigurableAppRuntime()
        .setDetails(OWNCLOUD_DETAILS)
        .setAuthentication(JsonUtil.fromJsonOrDie("{\n" +
                "        \"login_fields\": {\n" +
                "            \"user\": \"{{account.name}}\",\n" +
                "            \"password\": \"{{account.password}}\",\n" +
                "            \"remember_login\": \"1\",\n" +
                "            \"timezone-offset\": \"{{timezone-offset}}\",\n" +
                "            \"requesttoken\": \"pass\"\n" +
                "        },\n" +
                "        \"home_path\": \"index.php\",\n" +
                "        \"login_path\": \"index.php\",\n" +
                "        \"login_page_markers\": [ \"<form method=\\\"post\\\" name=\\\"login\\\">\", \"class=\\\"login primary\\\"\" ]\n" +
                "    }", AppAuthConfig.class));

    public static final Map<String, AppRuntime> APPS_BY_NAME = initAppMap();

    private static final int MAX_REDIRECTS = 10;

    private static Map<String, AppRuntime> initAppMap() {
        final Map<String, AppRuntime> appMap = new LinkedHashMap<>();
        appMap.put(APP_EMAIL, ROUNDCUBE);
        appMap.put(APP_CALENDAR, ROUNDCUBE_CAL);
        appMap.put(APP_FILES, OWNCLOUD);
        return appMap;
    }

    @Autowired private CloudOsConfiguration configuration;

    private final Jedis redis;

    public InstalledAppLoader () { redis = new Jedis(getRedisHost(), getRedisPort()); }

    // override these if necessary
    protected int getRedisPort() { return 6379; }
    protected String getRedisHost () { return "127.0.0.1"; }

    public static final Map<String, AppRuntimeDetails> APP_DETAILS_BY_NAME = initAppDetailsMap();

    private static Map<String, AppRuntimeDetails> initAppDetailsMap() {
        final Map<String, AppRuntimeDetails> appDetailsMap = new LinkedHashMap<>();
        appDetailsMap.put(APP_EMAIL, ROUNDCUBE_DETAILS);
        appDetailsMap.put(APP_CALENDAR, ROUNDCUBE_CAL_DETAILS);
        appDetailsMap.put(APP_FILES, OWNCLOUD_DETAILS);
        return appDetailsMap;
    }

    public Response loadApp(CloudOsAccount account, AppRuntime app, HttpContext context) throws IOException {

        final String publicUriBase = configuration.getPublicUriBase();
        final String appPath = getAppPath(publicUriBase, app.getDetails());
        final AppAuthConfig appAuth = app.getAuthentication();
        final String appHome = (appAuth == null) ? appPath : appAuth.getHome_path();
        final HttpRequestBean<String> requestBean = new HttpRequestBean<>(HttpMethods.GET, appPath);
        final CookieJar cookieJar = new CookieJar();

        // request the app home page, will load or will redirect us to a login page // this step often used to set cookies and other tokens
        BufferedResponse response = ProxyUtil.proxyResponse(requestBean, context, appHome, cookieJar);
        response = followRedirects(context, appPath, response, cookieJar);
        if (!response.isSuccess() || !app.isLoginPage(response.getDocument())) {
            // error, or user appears to be logged in, simply redirect to app page
            return sendToApp(appPath, appHome, cookieJar);
        }

        // attempt login and see what the app sends back
        log.info("attempting login for " + appPath + " account " + account.getName());
        final HttpRequestBean<String> authRequest = app.buildLoginRequest(account, response, context, appPath);
        response = ProxyUtil.proxyResponse(authRequest, context, appPath, cookieJar);
        if (!response.isSuccess() || app.isLoginPage(response.getDocument())) {
            log.warn("login failed, sending to main app page");
            return sendToApp(appPath, appHome, cookieJar);
        }

        String location;
        if (appAuth != null) {
            // follow a redirect if the Location matches the registration_redirect or login_redirect regex (if either are set)
            if (appAuth.hasRegistration_redirect() && response.is3xx()) {
                location = response.getFirstHeaderValue(HttpHeaders.LOCATION);
                if (appAuth.getRegistrationRedirectPattern().matcher(location).matches()) {

                    final BufferedResponse resolved = followRedirects(context, appPath, response, cookieJar);
                    if (resolved == null) {
                        log.warn("Too many redirects, sending to main app page");
                        return sendToApp(appPath, appHome, cookieJar);
                    }

                    // if this is a registration page, register ourselves...
                    if (resolved.isSuccess() && app.isRegistrationPage(resolved.getDocument())) {
                        final HttpRequestBean<String> registrationRequest = app.buildRegistrationRequest(account, resolved, context, appPath);
                        response = ProxyUtil.proxyResponse(registrationRequest, context, appPath, cookieJar);

                        if (!response.isSuccess() || app.isRegistrationPage(response.getDocument())) {
                            log.warn("registration failed, sending to main app page");
                        }
                        return sendToApp(appPath, appHome, cookieJar);
                    }

                } else if (appAuth.getLoginRedirectPattern().matcher(location).matches()) {

                    final BufferedResponse resolved = followRedirects(context, appPath, response, cookieJar);
                    if (resolved == null) {
                        log.warn("Too many redirects, sending to main app page");
                        return sendToApp(appPath, appHome, cookieJar);
                    }

                    if (resolved.isSuccess() && !app.isLoginPage(resolved.getDocument())) {
                        return sendToApp(appHome, resolved.getRequestUri(), cookieJar);
                    }
                } else {
                    // relay the redirect to the end user
                    return sendToApp(appPath, location, cookieJar);
                }
            }
        }

        return sendToApp(appPath, appHome, cookieJar);
    }

    public Response sendToApp(String appPath, String location, CookieJar cookieJar) {

        if (!appPath.endsWith("/")) appPath += "/";
        if (!(location.startsWith("http://") || location.startsWith("https://"))) location = appPath + location;

        final AuthTransition auth = new AuthTransition(location, cookieJar.getCookiesList());
        final String uuid = auth.getUuid();
        try {
            redis.set(uuid, JsonUtil.toJson(auth), "NX", "EX", TimeUnit.HOURS.toSeconds(24));
        } catch (Exception e) {
            log.error("sendToApp: Error writing to redis: "+e, e);
            return Response.serverError().build();
        }

        final String hostname = configuration.getHostname();
        String path = URIUtil.getHost(appPath).equals(hostname)
                ? URIUtil.toUri(configuration.getPublicUriBase()).getScheme()+"://"+hostname
                : appPath;
        if (!path.endsWith("/")) path += "/";

        Response.ResponseBuilder responseBuilder = Response.temporaryRedirect(URIUtil.toUri(path + "__cloudos__/api/app/auth/"+uuid));
        return responseBuilder.build();
    }

    public BufferedResponse followRedirects(HttpContext context, String appPath,
                                            BufferedResponse response, CookieJar cookieJar) throws IOException {
        // shouldn't happen, but just in case
        if (response == null) return null;

        // follow up to MAX_REDIRECTs trying to get to a 2xx response. track cookies along the way.
        int redirCount = 0;
        String location;
        while ((location = response.getRedirectUri()) != null) {
            final HttpRequestBean<String> redirect = new HttpRequestBean<>(appPath + location);
            response = ProxyUtil.proxyResponse(redirect, context, appPath, cookieJar);
            if (++redirCount > MAX_REDIRECTS) response = null;
        }
        return response;
    }

    private String getAppPath(String publicUriBase, AppRuntimeDetails details) {
        StringBuilder sb = new StringBuilder();
        if (details.hasHostname()) {
            sb.append(publicUriBase.replace("://", "://"+details.getHostname()+"-"));
        } else {
            sb.append(publicUriBase);
        }
        if (details.hasPath()) {
            // ensure a slash exists between the hostname and the path
            if (!publicUriBase.endsWith("/") && !details.getPath().startsWith("/")) sb.append("/");
            sb.append(details.getPath());
        }
        return sb.toString();
    }

}
