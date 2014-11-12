package cloudos.resources;

import cloudos.appstore.model.AppRuntime;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.app.AuthTransition;
import cloudos.service.app.InstalledAppLoader;
import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpCookieBean;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static cloudos.resources.ApiConstants.H_API_KEY;

@Path(ApiConstants.APP_ADAPTER_ENDPOINT)
@Service @Slf4j
public class AppAdapter {

    @Autowired private SessionDAO sessionDAO;
    @Autowired private AppDAO appDAO;
    @Autowired private InstalledAppLoader installedAppLoader;
    @Autowired private CloudOsConfiguration configuration;
    @Autowired private RedisService redis;

    @GET
    @Path("/load/{app}")
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

    @GET
    @Path("/auth/{uuid}")
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
            responseBuilder = responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toHeaderValue());
        }

        return responseBuilder.build();
    }

}
