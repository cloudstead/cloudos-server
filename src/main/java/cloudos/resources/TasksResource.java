package cloudos.resources;

import cloudos.service.RootyService;
import cloudos.service.task.CloudOsTaskResult;
import cloudos.service.task.TaskService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.RootyMessage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.TASKS_ENDPOINT)
@Service @Slf4j
public class TasksResource {

    @Autowired private TaskService taskService;
    @Autowired private RootyService rootyService;

    /**
     * Retrieve history for a background task
     * @param uuid The background task to look up
     * @return a TaskResult representing the history for the task
     */
    @GET
    @Path("/{uuid}")
    @ReturnType("cloudos.service.task.TaskResult")
    public Response getHistory (@PathParam("uuid") String uuid) {

        final CloudOsTaskResult result = taskService.getResult(uuid);

        if (result == null) return notFound(uuid);

        if (result.hasRootyUuid()) {
            final RootyMessage status = rootyService.getStatusManager().getStatus(result.getRootyUuid());
            if (status != null) result.setRootyStatus(status);
        }

        return ok(result);
    }

}
