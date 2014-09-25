package cloudos.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rooty.toots.service.ServiceKeyVendorMessage;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * For integration tests, this resource receives the rooty "vendor messages"
 * for service requests.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(MockServiceRequestsResource.ENDPOINT)
@Service @Slf4j
public class MockServiceRequestsResource {

    public static final String ENDPOINT = "/service_requests";

    @Getter private List<ServiceKeyVendorMessage> serviceRequests = new ArrayList<>();

    @POST
    public Response requestService (ServiceKeyVendorMessage request) {
        log.info("Received serviceKeyRequest: " + request.getHost());
        serviceRequests.add(request);
        return Response.ok().build();
    }

}
