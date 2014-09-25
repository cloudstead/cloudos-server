package cloudos.resources;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.SECURITY_ENDPOINT)
@Service @Slf4j
public class SecurityResource {

    @Autowired private SslCertificatesResource certificatesResource;
    @Path(ApiConstants.EP_CERTS)
    public SslCertificatesResource getCertificatesResource () { return certificatesResource; }

    @Autowired private ServiceKeysResource serviceKeysResource;
    @Path(ApiConstants.EP_SERVICE_KEYS)
    public ServiceKeysResource getServiceKeysResource() { return serviceKeysResource; }
}
