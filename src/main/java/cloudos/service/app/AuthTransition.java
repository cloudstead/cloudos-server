package cloudos.service.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.HttpCookieBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class AuthTransition {

    public static final String AUTH_COOKIE = "__cloudos_http_auth";
    public static final String SESSION_COOKIE = "__cloudos_http_session";

    @Getter @Setter private String uuid;
    @Getter @Setter private String sessionId;
    @Getter @Setter private String authHeaderValue;
    @Getter @Setter private String redirectUri = "/";
    @Getter @Setter private List<HttpCookieBean> cookies = new ArrayList<>();

    // for apps that use HTTP auth proxied via Apache, put the authHeaderValue here
    // so Apache can pass it as the Authorization header
    @JsonIgnore public HttpCookieBean getAuthCookie() {
        return new HttpCookieBean(AUTH_COOKIE, authHeaderValue).setSecure(true).setPath("/");
    }

    // for apps that use HTTP auth proxied via CloudOs, put the sessionId here
    // so the AppProxy can identify the user
    @JsonIgnore public HttpCookieBean getSessionCookie() {
        return new HttpCookieBean(SESSION_COOKIE, sessionId).setSecure(true).setPath("/");
    }

    public AuthTransition(String redirectUri) {
        this.uuid = UUID.randomUUID().toString();
        this.redirectUri = redirectUri;
    }

}
