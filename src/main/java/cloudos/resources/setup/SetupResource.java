package cloudos.resources.setup;

import cloudos.dao.AccountDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.support.SetupRequest;
import cloudos.resources.ApiConstants;
import cloudos.service.RootyService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.time.ImprovedTimezone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.toots.system.SystemSetTimezoneMessage;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.SETUP_ENDPOINT)
@Service
@Slf4j
public class SetupResource {

    @Autowired private AccountDAO accountDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private SetupSettingsSource setupSettingsSource;
    @Autowired private RootyService rooty;

    /**
     * Perform first-time cloudstead setup. Creates the first admin account.
     * @param request The SetupRequest
     * @return a CloudosAuthResponse
     * @statuscode 422 if the setup request fails validation
     */
    @POST
    @ReturnType("cloudos.model.auth.CloudOsAuthResponse")
    public Response setup (@Valid SetupRequest request) throws Exception {

        // only proceed if the setup key file is present and matches the request's setupKey
        setupSettingsSource.validateFirstTimeSetup(request);

        // create admin account in kerberos and cloud storage
        request.setAdmin(true);
        final Account account;
        try {
            account = accountDAO.create(request);
        } catch (Exception e) {
            log.error("Error saving admin account: "+e, e);
            return Response.serverError().build();
        }

        if (request.hasSystemTimeZone()) {
            final ImprovedTimezone timezone = ImprovedTimezone.getTimeZoneById(request.getSystemTimeZone());
            rooty.getSender().write(new SystemSetTimezoneMessage(timezone.getLinuxName()));
        }

        account.setPassword(request.getPassword()); // keep the password in the session
        final String sessionId = sessionDAO.create(account);

        setupSettingsSource.firstTimeSetupCompleted();

        return Response.ok(new CloudOsAuthResponse(sessionId, account)).build();
    }
}
