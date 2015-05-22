package cloudos.service.app;

import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppAuthConfig;
import cloudos.server.CloudOsConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.http.CookieJar;
import org.cobbzilla.util.http.HttpAuthType;
import org.cobbzilla.util.string.StringUtil;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true)
public class AppProxyContext {

    @Getter @Setter private String apiKey;
    @Getter @Setter private CloudOsConfiguration configuration;
    @Getter @Setter private CloudOsAccount account;
    @Getter @Setter private AppRuntime app;
    @Getter @Setter private CookieJar cookieJar = new CookieJar();
    @Setter private String location;

    public String getLocation () { return empty(location) ? getAppHome() : location; }

    public AppAuthConfig getAppAuth() { return app.getAuthentication(); }

    public String getAppPath () { return app.getDetails().getPath(configuration.getPublicUriBase()); }

    public String getAppHome () {
        if (!app.hasAuthentication()) return getAppPath();
        return getAppPath() + getAppAuth().getHome_path();
    }

    public String getAuthKey() {
        final String appPath = getAppPath();
        int qPos = appPath.indexOf("?");
        return account.getName()+"::"+(qPos == -1 ? appPath : appPath.substring(0, qPos));
    }

    public String getAuthHeaderValue() {
        final HttpAuthType authType = getAppAuth().getHttp_auth();
        switch (authType) {
            case basic:
                return StringUtil.urlEncode("Basic " + Base64.encodeBase64String((account.getName() + ":" + account.getPassword()).getBytes()));
            default:
                throw new IllegalArgumentException("Auth type not yet supported: "+authType);
        }
    }

    public boolean hasHttpAuth() {
        return app.hasAuthentication() && app.getAuthentication().getHttp_auth() != null;
    }
}
