package cloudos.resources;

import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.support.AppInstallUrlRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.AppInstallTask;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static cloudos.resources.ApiConstants.H_API_KEY;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.APPS_ENDPOINT)
@Service @Slf4j
public class AppsResource {

    @Autowired private SessionDAO sessionDAO;
    @Autowired private AppDAO appDAO;
    @Autowired private CloudOsConfiguration configuration;
    @Autowired private TaskService taskService;

    @GET
    @Path("/installed")
    public Response listInstalledApps (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        try {
            return Response.ok(appDAO.findActive()).build();
        } catch (Exception e) {
            log.error("Error finding installed apps: "+e, e);
            return Response.serverError().build();
        }
    }

    @POST
    public Response installAppFromUrl (@HeaderParam(H_API_KEY) String apiKey,
                                       AppInstallUrlRequest request)
            throws Exception { // todo: better exception handling, don't throw anything

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can install apps
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        // todo: return immediately with a handle and start background job; add api endpoint to query activity associated with the handle
        final AppInstallTask task = new AppInstallTask()
                .setAccount(admin)
                .setRequest(request)
                .setAppDAO(appDAO)
                .setConfiguration(configuration);

        final TaskId taskId = taskService.execute(task);

        return Response.ok(taskId).build();
    }

}
