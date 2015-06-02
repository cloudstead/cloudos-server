package cloudos.resources;

import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.server.CloudOsConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.DNS_ENDPOINT)
@Service @Slf4j
public class DnsRecordsResource {

    public static final String EP_DELETE = "/delete";

    @Autowired private SessionDAO sessionDAO;
    @Autowired private CloudOsConfiguration configuration;

    private DnsManager dnsManager() { return configuration.getDnsManager(); }

    /**
     * List all DNS records. Must be admin.
     * @param apiKey The session ID
     * @return A List of DnsRecords
     * @statuscode 403 if caller is not an admin
     */
    @GET
    @ReturnType("java.util.List<org.cobbzilla.util.dns.DnsRecord>")
    public Response listAllRecords (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final List<DnsRecord> records;
        try {
            records = dnsManager().list(new DnsRecordMatch().setSubdomain(configuration.getHostname()));

        } catch (ApiException e) {
            log.error("API Error listing DNS records: "+e, e);
            return ResourceUtil.toResponse(e);

        } catch (Exception e) {
            log.error("Error listing DNS records: "+e, e);
            return serverError();
        }

        return ok(records);
    }

    /**
     * Write a DNS record. Must be admin
     * @param apiKey The session ID
     * @param record The record to write
     * @return The record that was written
     * @statuscode 403 if caller is not an admin
     */
    @POST
    @ReturnType("org.cobbzilla.util.dns.DnsRecord")
    public Response writeRecord (@HeaderParam(H_API_KEY) String apiKey, DnsRecord record) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final boolean written;
        try {
            written = dnsManager().write(record);
            dnsManager().publish();

        } catch (ApiException e) {
            log.error("API Error writing DNS record: "+e, e);
            return ResourceUtil.toResponse(e);

        } catch (Exception e) {
            log.error("Error writing DNS record: "+e, e);
            return serverError();
        }
        return ok(written);
    }

    /**
     * Delete a DNS record. Must be admin
     * @param apiKey The session ID
     * @param match The records to match
     * @return The number of records removed
     * @statuscode 403 if caller is not an admin
     */
    @POST
    @Path(EP_DELETE)
    @ReturnType("java.lang.Integer")
    public Response deleteRecords (@HeaderParam(H_API_KEY) String apiKey, DnsRecordMatch match) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        match.setSubdomain(configuration.getHostname());

        final int removed;
        try {
            removed = dnsManager().remove(match);
            dnsManager().publish();

        } catch (ApiException e) {
            log.error("API Error removing DNS records: "+e, e);
            return ResourceUtil.toResponse(e);

        } catch (Exception e) {
            log.error("Error removing DNS record: "+e, e);
            return serverError();
        }
        return ok(removed);
    }

}
