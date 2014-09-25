package cloudos.model.app_impl;

import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.AppRuntimeBase;
import com.google.common.collect.Multimap;
import com.sun.jersey.api.core.HttpContext;
import lombok.NoArgsConstructor;
import cloudos.appstore.model.CloudOsAccount;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.wizard.util.BufferedResponse;

import static org.cobbzilla.util.string.StringUtil.urlEncode;

@NoArgsConstructor
public class OwncloudAppRuntime extends AppRuntimeBase {

    public OwncloudAppRuntime(AppRuntimeDetails details) {
        super(details, null);
    }

    @Override
    public boolean isLoginPage(String document) {
        // todo: make this check more robust
        return document.contains("<form method=\"post\" name=\"login\">")
                && document.contains("class=\"login primary\"");
    }

    @Override
    public HttpRequestBean<String> buildLoginRequest(CloudOsAccount account, BufferedResponse initialResponse, HttpContext context, String appPath) {

        final StringBuilder auth = new StringBuilder()
                .append("user=").append(urlEncode(account.getName()))
                .append("&password=").append(urlEncode(account.getPassword()))
                .append("&remember_login=1&timezone-offset=-7");

        final Multimap<String, String> headers = populateHeaders(initialResponse);

        return new HttpRequestBean<>(HttpMethods.POST, appPath, auth.toString(), headers);
    }
}
