package cloudos.resources;

import cloudos.dao.EmailDomainDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.EmailDomain;
import cloudos.server.CloudOsConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.events.email.NewEmailDomainEvent;
import rooty.events.email.RemoveEmailDomainEvent;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.EP_DOMAINS)
@Service @Slf4j
public class EmailDomainsResource {

    @Autowired private EmailDomainDAO emailDomainDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private CloudOsConfiguration configuration;

    /**
     * Find all email domains. Must be admin.
     * @param apiKey The session ID
     * @return a List of all email domains
     * @statuscode 403 if caller is not an admin
     */
    @GET
    @ReturnType("java.util.List<cloudos.model.EmailDomain>")
    public Response findAll(@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final List<EmailDomain> emailDomains = emailDomainDAO.findAll();
        final EmailDomain localDomain = new EmailDomain(configuration.getHostname(), true);
        if (!emailDomains.contains(localDomain)) emailDomains.add(localDomain);

        return Response.ok(emailDomains).build();
    }

    /**
     * Register a new email domain. Upon successful completion, the cloudstead will accept email for the new domain.
     * @param apiKey The session ID
     * @param domainName The email domain that the cloudstead should accept email for.
     * @return the EmailDomain that was registered
     */
    @PUT
    @Path("/{domain}")
    @ReturnType("cloudos.model.EmailDomain")
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

    /**
     * Remove an email domain.
     * @param apiKey The session ID
     * @param domainName The email domain to remove
     * @return "true" if the domain was successfully removed
     */
    @DELETE
    @Path("/{domain}")
    @ReturnType("java.lang.Boolean")
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
