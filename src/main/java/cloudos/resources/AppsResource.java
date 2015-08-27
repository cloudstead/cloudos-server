package cloudos.resources;

import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.config.AppConfiguration;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.app.AppRepositoryState;
import cloudos.model.app.CloudOsApp;
import cloudos.model.support.AppDownloadRequest;
import cloudos.model.support.AppUninstallRequest;
import cloudos.server.CloudOsConfiguration;
import org.cobbzilla.wizard.task.TaskId;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;
import static org.cobbzilla.wizard.resources.ResourceUtil.serverError;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.APPS_ENDPOINT)
@Service @Slf4j
public class AppsResource {

    @Autowired private CloudOsConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private AppDAO appDAO;

    /**
     * List all apps that are installed and active
     * @param apiKey The session ID
     * @return a List of InstalledApps
     */
    @GET
    @ReturnType("java.util.List<cloudos.model.app.CloudOsApp>")
    public Response listActiveApps (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        final List<CloudOsApp> apps;
        try {
            apps = appDAO.findActive();
        } catch (Exception e) {
            log.error("Error finding installed apps: "+e, e);
            return serverError();
        }

        // if account is not admin, scrub out databags
        if (!admin.isAdmin()) {
            for (CloudOsApp app : apps) app.setDatabags(null);
        }

        return ok(apps);
    }

    /**
     * List ALL apps in the app repository
     * @param apiKey The session ID
     * @return the state of the AppRepository
     */
    @GET
    @Path("/all")
    @ReturnType("cloudos.model.app.AppRepositoryState")
    public Response listAllApps (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        final AppRepositoryState state;
        try {
            state = appDAO.getAppRepositoryState();
        } catch (Exception e) {
            log.error("Error finding installed apps: "+e, e);
            return serverError();
        }

        return ok(state);
    }

    /**
     * Refresh the list of apps. Must be admin or supply proper key
     * @param apiKey The session ID
     * @param refreshKey The key to refresh the apps
     * @statuscode 403 if caller is not an admin or key is wrong
     */
    @GET
    @Path(ApiConstants.EP_REFRESH)
    @ReturnType("java.lang.Void")
    public Response refresh (@HeaderParam(H_API_KEY) String apiKey,
                             @QueryParam("refreshKey") String refreshKey) {

        if (!empty(refreshKey)) {
            if (!refreshKey.equals(configuration.getAppRefreshKey())) return ResourceUtil.forbidden();

        } else {
            final Account admin = sessionDAO.find(apiKey);
            if (admin == null) return ResourceUtil.notFound(apiKey);

            // only admins can refresh apps
            if (!admin.isAdmin()) return ResourceUtil.forbidden();
        }

        appDAO.resetApps();
        return ok();
    }

    /**
     * Download a new app from a URL. Must be admin
     * @param apiKey The session ID
     * @param request The installation request
     * @statuscode 403 if caller is not an admin
     * @return a TaskId, can be used to check installation progress
     */
    @POST
    @Path("/download")
    @ReturnType("cloudos.service.task.TaskId")
    public Response downloadApp (@HeaderParam(H_API_KEY) String apiKey,
                                 AppDownloadRequest request) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can download apps
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final TaskId taskId = appDAO.download(admin, request);

        return ok(taskId);
    }

    /**
     * Retrieve configuration options for an app
     * @param apiKey The session ID
     * @param app The app name
     * @param version The app version
     * @return the AppConfiguration
     * @statuscode 403 if caller is not an admin
     */
    @GET
    @Path("/apps/{app}/versions/{version}/config")
    @ReturnType("cloudos.appstore.model.app.config.AppConfiguration")
    public Response getConfiguration (@HeaderParam(H_API_KEY) String apiKey,
                                      @PathParam("app") String app,
                                      @PathParam("version") String version) {
        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can view app configs
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final String locale = configuration.getLocale(admin);
        final AppConfiguration config = appDAO.getConfiguration(app, version, locale);
        if (config == null) return ResourceUtil.notFound(app+"/"+version);

        return ok(config);
    }

    /**
     * Write configuration options for an app.
     * @param apiKey The session ID
     * @param app The app name
     * @param version The app version
     * @param config The updated app configuration. Anything not specified in this object will not be changed.
     * @return Just an HTTP status code
     * @statuscode 403 if caller is not an admin
     */
    @POST
    @Path("/apps/{app}/versions/{version}/config")
    @ReturnType("java.lang.Void")
    public Response setConfiguration (@HeaderParam(H_API_KEY) String apiKey,
                                      @PathParam("app") String app,
                                      @PathParam("version") String version,
                                      AppConfiguration config) {
        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can view app configs
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        appDAO.setConfiguration(app, version, config);
        return ok();
    }

    /**
     * Install an application. Must already be downloaded and configured (if configuration is required).
     * @param apiKey The session ID
     * @param app The app name
     * @param version The app version, or the special string 'latest' to use the latest version
     * @param force If true, force installation even if the same version is already installed
     * @statuscode 403 if caller is not an admin
     * @return a TaskId, can be used to check installation progress
     */
    @POST
    @Path("/apps/{app}/versions/{version}/install")
    @ReturnType("cloudos.service.task.TaskId")
    public Response installApp (@HeaderParam(H_API_KEY) String apiKey,
                                @PathParam("app") String app,
                                @PathParam("version") String version,
                                @QueryParam("force") Boolean force) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can install apps
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        if (version.equals(AppManifest.LATEST_VERSION)) version = null;
        final TaskId taskId = appDAO.install(admin, app, version, force == null ? false : force);

        return ok(taskId);
    }

    /**
     * Uninstall an application.
     * @param apiKey The session ID
     * @param app The app name
     * @param request Determines how much stuff to delete. @see cloudos.model.support.AppUninstallMode
     * @statuscode 403 if caller is not an admin
     * @return a TaskId, can be used to check uninstall progress
     */
    @POST
    @Path("/apps/{app}/uninstall")
    @ReturnType("cloudos.service.task.TaskId")
    public Response uninstallApp (@HeaderParam(H_API_KEY) String apiKey,
                                  @PathParam("app") String app,
                                  AppUninstallRequest request) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can uninstall apps
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        // copy URL fields to request object
        request.setName(app);

        final TaskId taskId = appDAO.uninstall(admin, request);

        return ok(taskId);
    }
}
