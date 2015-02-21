package cloudos.resources;

import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.filter.AppFilter;
import cloudos.appstore.model.app.filter.AppFilterConfig;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.databag.PortsDatabag;
import cloudos.model.Account;
import cloudos.model.app.AppConfiguration;
import cloudos.model.app.CloudOsApp;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.app.AuthTransition;
import cloudos.service.app.InstalledAppLoader;
import com.qmino.miredot.annotations.ReturnType;
import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.*;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.cobbzilla.wizard.util.BufferedResponse;
import org.cobbzilla.wizard.util.ProxyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.wizard.util.HttpContextUtil.getQueryParams;

@Path(ApiConstants.APP_ADAPTER_ENDPOINT)
@Service @Slf4j
public class AppAdapter {

    @Autowired private SessionDAO sessionDAO;
    @Autowired private AppDAO appDAO;
    @Autowired private InstalledAppLoader installedAppLoader;
    @Autowired private CloudOsConfiguration configuration;
    @Autowired private RedisService redis;

    /**
     * Load a CloudOs application.
     * @param apiKey The session ID
     * @param context The HTTP context
     * @param appName name of the app to load
     * @return a 302 redirect
     * @statuscode 302 a redirect to the AuthRelay URL
     */
    @GET
    @Path("/load/{app}")
    @ReturnType("java.lang.Void")
    public Response loadApp(@QueryParam(H_API_KEY) String apiKey,
                            @Context HttpContext context,
                            @PathParam("app") String appName) throws IOException {

        long start = System.currentTimeMillis();
        try {
            final Account account = sessionDAO.find(apiKey);
            if (account == null) return ResourceUtil.forbidden();

            // Based on the app, find the base uri
            final AppRuntime app = appDAO.findAppRuntime(appName);

            // Hit up that URL -- do we get a login screen?
            return installedAppLoader.loadApp(apiKey, account, app, context);

        } catch (Exception e) {
            // todo: better error reporting
            log.error("loadApp: error loading: " + e, e);
            return Response.serverError().build();

        } finally {
            log.info("loadApp executed in " + TimeUtil.formatDurationFrom(start));
        }
    }

    /**
     * The AuthRelay. Sends app-specific cookies to the caller, then redirects to the app.
     * @param context The HTTP context
     * @param uuid The AuthRelay UUID, returned in the 302 redirect from a call to /load/{app}
     * @return 302 redirect to the app
     */
    @GET
    @Path("/auth/{uuid}")
    @ReturnType("java.lang.Void")
    public Response authRelay(@Context HttpContext context,
                              @PathParam("uuid") String uuid) {
        final AuthTransition auth;
        try {
            auth = JsonUtil.fromJson(redis.get(uuid), AuthTransition.class);
        } catch (Exception e) {
            redis.del(uuid);
            log.error("authRelay: error looking up AuthTransition: "+e);
            return Response.temporaryRedirect(URIUtil.toUri(configuration.getPublicUriBase())).build();
        }

        Response.ResponseBuilder responseBuilder = Response.temporaryRedirect(URIUtil.toUri(auth.getRedirectUri()));
        for (HttpCookieBean cookie : auth.getCookies()) {
            cookie.setPath("/"); // ensure all cookies get sent
            responseBuilder = responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toHeaderValue());
        }

        return responseBuilder.build();
    }

    /**
     * Filter a GET request for an application page
     * @param context The HTTP context
     * @param appName name of the app
     * @param uri The URI to proxy and filter
     * @return what the app would have returned, but filtered
     * @statuscode what the app would have returned anyway
     */
    @GET
    @Path("/filter/{app}/{uri : .+}")
    @ReturnType("javax.ws.rs.core.StreamingOutput")
    public Response filterGet(@Context HttpContext context,
                              @PathParam("app") String appName,
                              @PathParam("uri") String uri) throws IOException {
        return filter(context, appName, uri, HttpMethods.GET);
    }

    /**
     * Filter a POST request for an application page
     * @param context The HTTP context
     * @param appName name of the app
     * @param uri The URI to proxy and filter
     * @return what the app would have returned, but filtered
     * @statuscode what the app would have returned anyway
     */
    @POST
    @Path("/filter/{app}/{uri : .+}")
    @ReturnType("javax.ws.rs.core.StreamingOutput")
    public Response filterPost(@Context HttpContext context,
                               @PathParam("app") String appName,
                               @PathParam("uri") String uri) throws IOException {
        return filter(context, appName, uri, HttpMethods.POST);
    }

    public Response filter(HttpContext context,
                           String appName,
                           String uri,
                           String method) throws IOException {

        final CloudOsApp app = appDAO.findInstalledByName(appName);
        if (app == null) return ResourceUtil.notFound();

        final AppManifest manifest = app.getManifest();
        final PortsDatabag ports = configuration.getAppLayout(manifest).getPortsDatabag();
        if (ports == null) return ResourceUtil.notFound();

        final String appUri = "http://127.0.0.1:" + ports.getPrimary() + manifest.getNormalizedLocalMount();
        final String proxyUri = appUri + uri + getQueryParams(context);
        final CookieJar cookieJar = new CookieJar();
        final HttpRequestBean<String> requestBean = new HttpRequestBean<>(method, proxyUri);
        final BufferedResponse response = ProxyUtil.proxyResponse(requestBean, context, appUri, cookieJar);

        if (manifest.hasFilters()) {

            final AppFilterConfig filterConfig = manifest.getFilterConfig(uri);
            if (filterConfig != null && filterConfig.hasFilters() && response.hasDocument()) {

                final AppFilter[] filters = filterConfig.getFilters();
                final AppRuntimeDetails runtime = appDAO.findAppRuntime(appName).getDetails().getDetails(configuration.getPublicUriBase());
                final AppConfiguration appConfig = appDAO.getConfiguration(appName, manifest.getVersion());

                // these things define the {{ }} language used in a manifest file's web.filter values to insert
                final Map<String, Object> scope = new HashMap<>();
                scope.put("config", configuration);
                scope.put("app", runtime);
                scope.put("databags", appConfig.getDatabagMap());
                String document = response.getDocument();

                try {
                    for (AppFilter filter : filters) {
                        document = filter.getHandler().apply(document, scope);
                    }
                    return response.withNewDocument(document);

                } catch (Exception e) {
                    log.error("Error applying filter fields (uri=" + uri + "): " + e, e);
                }
            }
        }

        return response.getResponse();
    }

}
