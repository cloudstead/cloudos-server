package cloudos.service;

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

    @Getter @Setter private String uuid;
    @Getter @Setter private String redirectUri = "/";
    @Getter @Setter private List<HttpCookieBean> cookies = new ArrayList<>();

    public AuthTransition(String redirectUri, List<HttpCookieBean> cookies) {
        this.uuid = UUID.randomUUID().toString();
        this.redirectUri = redirectUri;
        this.cookies = cookies;
    }

}
