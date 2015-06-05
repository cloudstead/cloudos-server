package cloudos.service.appstore;

import cloudos.appstore.ApiConstants;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.server.CloudOsConfiguration;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static cloudos.resources.ApiConstants.H_API_KEY;

@Service @Slf4j
public class AppStoreApiProxy extends HttpHandler {

    @Autowired private SessionDAO sessionDAO;
    @Autowired private CloudOsConfiguration configuration;

    @Override public void service(Request request, Response response) throws Exception {

        final String apiKey = request.getHeader(H_API_KEY);
        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) {
            response.setStatus(HttpStatusCodes.FORBIDDEN, H_API_KEY+" request header not found");
            return;
        }

        final HttpRequestBean requestBean = new HttpRequestBean<>()
                .setMethod(request.getMethod().getMethodString())
                .setUri(request.getRequestURI());

        for (String name : request.getHeaderNames()) {
            for (String value : request.getHeaders(name)) {
                requestBean.setHeader(name, value);
            }
        }

        // Add the appstore auth token
        requestBean.setHeader(ApiConstants.H_TOKEN, configuration.getAppStoreClient().getToken());

        final InputStream in = request.getInputStream();
        if (in != null && in.available() > 0) requestBean.setData(in);

        // get the appstore client (this will authenticate us if needed)
        final HttpClient httpClient = configuration.getAppStoreClient().getHttpClient();

        // call the appstore API
        final HttpResponseBean responseBean = HttpUtil.getResponse(requestBean, httpClient);

        // relay the response from HttpResponseBean -> Grizzly Response
        response.setStatus(responseBean.getStatus());

        final Multimap<String, String> headers = responseBean.getHeaders();
        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                response.addHeader(name, value);
            }
        }

        if (responseBean.hasEntity()) {
            StreamUtil.copyLarge(new ByteArrayInputStream(responseBean.getEntity()), response.getOutputStream());
        }

        // done
        response.flush();
    }
}
