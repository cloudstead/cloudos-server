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
@Path(ApiConstants.EMAIL_ENDPOINT)
@Service @Slf4j
public class EmailResource {

    @Autowired private EmailDomainsResource domainsResource;
    @Path(ApiConstants.EP_DOMAINS)
    public EmailDomainsResource getDomainsResource() { return domainsResource; }

}