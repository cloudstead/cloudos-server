package cloudos.resources;

import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.wizard.resources.ResourceUtil.forbidden;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.DNS_ENDPOINT)
@Service @Slf4j
public class DnsRecordsResource {

    public static final String EP_DELETE = "/delete";

    @Autowired private SessionDAO sessionDAO;
    @Autowired private CloudOsConfiguration configuration;

    private DnsManager dnsManager() { return configuration.getDnsManager(); }

    @GET
    public Response listAllRecords (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final List<DnsRecord> records;
        try {
            records = dnsManager().list(new DnsRecordMatch().setSubdomain(configuration.getHostname()));
        } catch (Exception e) {
            log.error("Error listing DNS records: "+e, e);
            return Response.serverError().build();
        }

        return Response.ok(records).build();
    }

    @POST
    public Response writeRecord (@HeaderParam(H_API_KEY) String apiKey, DnsRecord record) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final boolean written;
        try {
            written = dnsManager().write(record);
            dnsManager().publish();
        } catch (Exception e) {
            log.error("Error writing DNS record: "+e, e);
            return Response.serverError().build();
        }
        return Response.ok(written).build();
    }

    @POST
    @Path(EP_DELETE)
    public Response deleteRecords (@HeaderParam(H_API_KEY) String apiKey, DnsRecordMatch match) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        match.setSubdomain(configuration.getHostname());

        final int removed;
        try {
            removed = dnsManager().remove(match);
            dnsManager().publish();

        } catch (Exception e) {
            log.error("Error writing DNS record: "+e, e);
            return Response.serverError().build();
        }
        return Response.ok(removed).build();
    }

}
