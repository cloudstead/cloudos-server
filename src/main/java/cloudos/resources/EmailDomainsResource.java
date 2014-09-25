package cloudos.resources;

import cloudos.dao.EmailDomainDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.EmailDomain;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.events.email.NewEmailDomainEvent;
import rooty.events.email.RemoveEmailDomainEvent;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.EP_DOMAINS)
@Service @Slf4j
public class EmailDomainsResource {

    @Autowired private EmailDomainDAO emailDomainDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private CloudOsConfiguration configuration;

    @GET
    public Response findAll(@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        return Response.ok(emailDomainDAO.findAll()).build();
    }

    @PUT
    @Path("/{domain}")
    public Response create(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                           @PathParam("domain") String domainName) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        // Announce a new domain on the event bus
        final String name = domainName.toLowerCase();
        configuration.getRooty().getSender().write(new NewEmailDomainEvent(name));

        final EmailDomain emailDomain = new EmailDomain();
        emailDomain.setName(name);
        return Response.ok(emailDomainDAO.create(emailDomain)).build();
    }

    @DELETE
    @Path("/{domain}")
    public Response remove(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                           @PathParam("domain") String domainName) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final String name = domainName.toLowerCase();

        final EmailDomain emailDomain = emailDomainDAO.findByName(name);
        if (emailDomain == null) return ResourceUtil.notFound(name);

        // Announce removed domain on the event bus
        configuration.getRooty().getSender().write(new RemoveEmailDomainEvent(name));

        emailDomainDAO.delete(emailDomain.getUuid());

        return Response.ok(Boolean.TRUE).build();
    }

}
