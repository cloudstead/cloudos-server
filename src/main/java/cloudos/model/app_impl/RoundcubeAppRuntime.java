package cloudos.model.app_impl;

import cloudos.appstore.model.AppRuntimeBase;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.CloudOsAccount;
import com.google.common.collect.Multimap;
import com.sun.jersey.api.core.HttpContext;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.xml.XPathUtil;
import org.cobbzilla.wizard.util.BufferedResponse;

import java.io.ByteArrayInputStream;

import static org.cobbzilla.util.string.StringUtil.urlEncode;

@Slf4j
@NoArgsConstructor
public class RoundcubeAppRuntime extends AppRuntimeBase {

    public RoundcubeAppRuntime(AppRuntimeDetails details) {
        super(details, null);
    }

    @Override
    public boolean isLoginPage(String document) {
        // log.info("examining response for login elements:" + document); // this produces HUGE html docs in your logs.
        // sanity check
        if (document == null) {
            log.warn("isLoginPage: received null document");
            return false;
        }
        return document.contains("Username") && document.contains("Password") && document.contains("value=\"Login\"");
    }

    @Override
    public HttpRequestBean<String> buildLoginRequest(CloudOsAccount account, BufferedResponse initialResponse, HttpContext context, String appPath) {
        final StringBuilder auth = new StringBuilder()
                .append("_token=").append(urlEncode(getToken(initialResponse)))
                .append("&_task=login")
                .append("&_action=login")
                .append("&_timezone=").append(urlEncode("America/Los_Angeles"))
//                .append("&_dstactive=1")
                .append("&_url=")
                .append("&_user=").append(urlEncode(account.getName()))
                .append("&_pass=").append(urlEncode(account.getPassword()));

        final Multimap<String, String> headers = populateHeaders(initialResponse);

        final String uri = appPath + "?_task=login";
        return new HttpRequestBean<>(HttpMethods.POST, uri, auth.toString(), headers);
    }

    private String getToken(BufferedResponse response) {
        try {
            return new XPathUtil("//input[@name='_token']/@value").getFirstMatchText(new ByteArrayInputStream(response.getDocument().getBytes()));
        } catch (Exception e) {
            log.error("getToken: XPath wasn't found in document? "+e, e);
            throw new IllegalStateException(e);
        }
    }
}
