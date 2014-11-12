package cloudos.resources;

import cloudos.appstore.model.AppRuntime;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.InstalledApp;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.app.AuthTransition;
import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.cobbzilla.wizard.util.ProxyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static org.cobbzilla.util.string.StringUtil.empty;

@Path(ApiConstants.APP_PROXY_ENDPOINT) @Produces @Consumes
@Service @Slf4j
public class AppProxy {

    @Autowired private AppDAO appDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private CloudOsConfiguration configuration;

    @GET
    @Path("/{app}/{path:.*}")
    public Response get (@Context HttpContext context,
                         @PathParam("app") String app,
                         @PathParam("path") String path) {

        return proxySimpleRequest(HttpMethods.GET, context, app, path);
    }

    @POST
    @Path("/{app}/{path:.*}")
    public Response post (@Context HttpContext context,
                          @PathParam("app") String app,
                          @PathParam("path") String path,
                          InputStream dataStream) {
        return proxyEntityRequest(HttpMethods.POST, context, app, path, dataStream);
    }

    @PUT
    @Path("/{app}/{path:.*}")
    public Response put (@Context HttpContext context,
                         @PathParam("app") String app,
                         @PathParam("path") String path,
                         InputStream dataStream) {
        return proxyEntityRequest(HttpMethods.PUT, context, app, path, dataStream);
    }

    @HEAD
    @Path("/{app}/{path:.*}")
    public Response head (@Context HttpContext context,
                          @PathParam("app") String app,
                          @PathParam("path") String path) {
        return proxySimpleRequest(HttpMethods.HEAD, context, app, path);
    }

    @DELETE
    @Path("/{app}/{path:.*}")
    public Response delete (@Context HttpContext context,
                            @PathParam("app") String app,
                            @PathParam("path") String path) {
        return proxySimpleRequest(HttpMethods.DELETE, context, app, path);
    }

    private Response proxySimpleRequest(String scheme, HttpContext context, String app, String path) {
        log.info(scheme+": "+context);

        final AppRuntime runtime = appDAO.findAppRuntime(app);
        if (runtime == null) return ResourceUtil.notFound(app);

        final String sessionId = getSessionId(context);
        if (sessionId == null) return redirect(configuration.getPublicUriBase());

        final Account account = sessionDAO.find(sessionId);
        if (account == null) return redirect(configuration.getPublicUriBase());

        if (!path.startsWith("/")) path = "/"+path;
        final HttpRequestBean<String> requestBean = new HttpRequestBean<String>(scheme, path)
                .setAuthType(runtime.getAuthentication().getHttp_auth())
                .setAuthUsername(account.getName())
                .setAuthPassword(account.getPassword());

        return proxy(requestBean, runtime, context);
    }


    private Response proxyEntityRequest(String method, HttpContext context, String app, String path, InputStream dataStream) {
        log.info(method+": "+context);

        final AppRuntime runtime = appDAO.findAppRuntime(app);
        if (runtime == null) return ResourceUtil.notFound(app);

        final String sessionId = getSessionId(context);
        if (sessionId == null) return redirect(configuration.getPublicUriBase());

        final Account account = sessionDAO.find(sessionId);
        if (account == null) return redirect(configuration.getPublicUriBase());

        if (!path.startsWith("/")) path = "/"+path;
        final HttpRequestBean<InputStream> requestBean = new HttpRequestBean<InputStream>(method, path)
                .setAuthType(runtime.getAuthentication().getHttp_auth())
                .setAuthUsername(account.getName())
                .setAuthPassword(account.getPassword())
                .setData(dataStream);

        return proxy(requestBean, runtime, context);
    }

    private Response proxy(HttpRequestBean requestBean, AppRuntime runtime, HttpContext context) {
        final InstalledApp app = appDAO.findByName(runtime.getDetails().getName());
        final String appHome = app.getLocalBaseUri();
        requestBean.setUri(app.getLocalBaseUri()+requestBean.getUri());
        final String query = context.getRequest().getRequestUri().getRawQuery();
        if (!empty(query)) {
            requestBean.setUri(requestBean.getUri()+"?"+query);
            log.info("added query ("+query+"), uri now="+requestBean.getUri());
        }

        try {
            return ProxyUtil.streamProxy(requestBean, context, appHome);
        } catch (IOException e) {
            log.error("Error proxying ("+requestBean+"): "+e, e);
            return Response.serverError().build();
        }
    }

    private String getSessionId(HttpContext context) {
        try {
            return context.getRequest().getCookies().get(AuthTransition.SESSION_COOKIE).getValue();
        } catch (Exception e) {
            log.warn("getSessionId: No "+AuthTransition.AUTH_COOKIE +" cookie found: " + context.getRequest() + ": " + e);
            return null;
        }
    }

    private Response redirect(String path) { return Response.temporaryRedirect(URIUtil.toUri(path)).build(); }

}
