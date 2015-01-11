package cloudos.resources.setup;

import cloudos.dao.AccountDAO;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.support.RestoreRequest;
import cloudos.model.support.SetupRequest;
import cloudos.model.support.SetupResponse;
import cloudos.resources.ApiConstants;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.RestoreTask;
import cloudos.service.RootyService;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.time.ImprovedTimezone;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.toots.restore.GetRestoreKeyMessage;
import rooty.toots.system.SystemSetTimezoneMessage;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.SETUP_ENDPOINT)
@Service
@Slf4j
public class SetupResource {

    public static final long GET_RESTORE_KEY_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Autowired private AccountDAO accountDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private SetupSettingsSource setupSettingsSource;
    @Autowired private AppDAO appDAO;
    @Autowired private TaskService taskService;
    @Autowired private RootyService rootyService;
    @Autowired private CloudOsConfiguration configuration;

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
        final String backupKey = setupSettingsSource.validateFirstTimeSetup(request);

        // create admin account in kerberos and cloud storage
        request.setAdmin(true);
        final Account account;
        try {
            account = accountDAO.create(request);
        } catch (Exception e) {
            log.error("Error saving admin account: "+e, e);
            return Response.serverError().build();
        }

        // initial account does not require email validation
        account.setEmailVerified(true);
        account.setLastLogin();
        try {
            accountDAO.update(account);
        } catch (Exception e) {
            log.error("Error activating admin account: "+e, e);
            return Response.serverError().build();
        }

        if (request.hasSystemTimeZone()) {
            final ImprovedTimezone timezone = ImprovedTimezone.getTimeZoneById(request.getSystemTimeZone());
            rootyService.getSender().write(new SystemSetTimezoneMessage(timezone.getLinuxName()));
        }

        account.setPassword(request.getPassword()); // keep the password in the session
        final String sessionId = sessionDAO.create(account);

        setupSettingsSource.firstTimeSetupCompleted();

        // Load available apps
        account.setAvailableApps(new ArrayList<>(appDAO.getAvailableAppDetails().values()));

        // Generate the first-time setup response, which will include the restoreKey
        final SetupResponse response = new SetupResponse(sessionId, account, configuration, backupKey);
        log.debug("restore key is:" + response.getRestoreKey());

        return Response.ok(response).build();
    }

    /**
     * Get the restore key
     * @param apiKey The session ID
     * @return The base64-encoded restore key
     */
    @GET
    @Path("/key")
    @ReturnType("java.lang.String")
    public Response getRestoreKey(@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can get the restore key
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final String backupKey = rootyService.request(new GetRestoreKeyMessage(), GET_RESTORE_KEY_TIMEOUT).getResults();
        final String restoreKey = SetupResponse.generateRestoreKey(configuration, backupKey);
        return Response.ok(restoreKey).build();
    }

    /**
     * Restores a cloudstead from a previous backup
     * @param restoreRequest The RestoreRequest, includes the Base64-encoded restore key generated above during first-time setup.
     * @return a TaskId that can be used to check on the status of the restore operation
     */
    @POST
    @Path("/restore")
    @ReturnType("cloudos.service.task.TaskId")
    public Response restore (@Valid RestoreRequest restoreRequest) throws Exception {

        // only proceed if the setup key file is present and matches the request's setupKey
        final SetupRequest setup = new SetupRequest()
                .setSetupKey(restoreRequest.getSetupKey())
                .setInitialPassword(restoreRequest.getInitialPassword());
        setupSettingsSource.validateFirstTimeSetup(setup);

        final RestoreTask restoreTask = new RestoreTask()
                .setRootyService(rootyService)
                .setRestoreKey(restoreRequest.getRestoreKey())
                .setRestoreDatestamp(restoreRequest.getRestoreDatestamp())
                .setNotifyEmail(restoreRequest.getNotifyEmail());

        final TaskId taskId = taskService.execute(restoreTask);

        return Response.ok(taskId).build();

    }
}
