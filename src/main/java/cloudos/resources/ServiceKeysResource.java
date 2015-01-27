package cloudos.resources;

import cloudos.dao.ServiceKeyDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.ServiceKey;
import cloudos.service.RootyService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.toots.service.ServiceKeyRequest;
import rooty.toots.service.ServiceKeyVendorMessage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static rooty.toots.service.ServiceKeyRequest.Operation.DESTROY;
import static rooty.toots.service.ServiceKeyRequest.Operation.GENERATE;
import static rooty.toots.service.ServiceKeyRequest.Recipient.CUSTOMER;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.EP_SERVICE_KEYS)
@Service @Slf4j
public class ServiceKeysResource {

    @Autowired private ServiceKeyDAO serviceKeyDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private RootyService rooty;

    /**
     * List all ServiceKeys. Must be admin
     * @param apiKey The session ID
     * @return a List of ServiceKeys
     * @statuscode 403 if caller is not an admin
     */
    @GET
    @ReturnType("java.util.List<cloudos.model.ServiceKey>")
    public Response findServiceKeys (@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final List<ServiceKey> keys = serviceKeyDAO.findAll();
        return Response.ok(keys).build();
    }

    /**
     * Find a single ServiceKey. Must be admin
     * @param apiKey The session ID
     * @param name the name of the key
     * @return The ServiceKey
     * @statuscode 403 if caller is not an admin
     */
    @GET
    @Path("/{name}")
    @ReturnType("cloudos.model.ServiceKey")
    public Response findServiceKey (@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                     @PathParam("name") String name) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        ServiceKey found = serviceKeyDAO.findByName(name);
        if (found == null) return ResourceUtil.notFound(name);

        return Response.ok(found).build();
    }

    private static final long GEN_KEY_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    /**
     * Generate a new ServiceKey. Must be admin
     * @param apiKey The session ID
     * @param name The name of the service key
     * @param request The ServiceKeyRequest
     * @return Just an HTTP status code
     * @statuscode 403 if caller is not an admin
     */
    @POST
    @Path("/{name}")
    @ReturnType("java.lang.Void")
    public Response generateKey(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                @PathParam("name") String name,
                                ServiceKeyRequest request) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        if (!name.equals(request.getName())) return ResourceUtil.invalid();

        final ServiceKey found = serviceKeyDAO.findByName(name);
        if (found != null) throw new SimpleViolationException("{err.serviceKey.exists}", "a key with that name already exists");

        ServiceKeyRequest message = new ServiceKeyRequest()
                .setName(name)
                .setOperation(GENERATE)
                .setRecipient(request.getRecipient());

        message = (ServiceKeyRequest) rooty.request(message);

        if (message != null) {
            if (message.isSuccess() && message.getErrorCount() == 0) {
                if (request.getRecipient() == CUSTOMER) {
                    return Response.ok(new ServiceKeyVendorMessage().setKey(message.getResults())).build();
                } else {
                    return Response.ok().build();
                }
            } else {
                // key request failed -- perhaps because cloudstead is still locked
                log.error("Error generating service key: "+message.getResults());
                return ResourceUtil.invalid(message.getResults());
            }
        }

        log.error("Timeout generating service key, message UUID: "+message.getUuid());
        return Response.serverError().build();
    }

    /**
     * Delete a ServiceKey. Must be admin
     * @param apiKey The session ID
     * @param name The name of the service key
     * @return Just an HTTP status code
     * @statuscode 403 if caller is not an admin
     */
    @DELETE
    @Path("/{name}")
    public Response destroyKey(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                               @PathParam("name") String name) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final ServiceKey found = serviceKeyDAO.findByName(name);
        if (found == null) return ResourceUtil.notFound(name);

        try {
            rooty.request(new ServiceKeyRequest().setName(name).setOperation(DESTROY));
        } catch (Exception e) {
            return Response.serverError().entity("request timed out:"+e).build();
        }
        return Response.ok().build();
    }

}
