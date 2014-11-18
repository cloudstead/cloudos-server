package cloudos.resources;

import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import cloudos.service.task.TaskResult;
import cloudos.service.task.TaskService;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.TASKS_ENDPOINT)
@Service @Slf4j
public class TasksResource {

    @Autowired private TaskService taskService;

    /**
     * Retrieve history for a background task
     * @param uuid The background task to look up
     * @return a TaskResult representing the history for the task
     */
    @GET
    @Path("/{uuid}")
    @ReturnType("cloudos.service.task.TaskResult")
    public Response getHistory (@PathParam("uuid") String uuid) {

        final TaskResult result = taskService.getResult(uuid);

        if (result == null) return ResourceUtil.notFound(uuid);

        return Response.ok(result).build();
    }

}
