package cloudos.resources;

import cloudos.dao.AccountDAO;
import cloudos.dao.AccountDeviceDAO;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.AccountDevice;
import cloudos.model.AccountLoginRequest;
import cloudos.model.support.*;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.TemplatedMailService;
import cloudos.service.TwoFactorAuthService;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.ACCOUNTS_ENDPOINT)
@Service @Slf4j
public class AccountsResource {

    private Response serverError() { return Response.serverError().build(); }

    @Autowired private AccountDAO accountDAO;
    @Autowired private AccountDeviceDAO deviceDAO;
    @Autowired private AppDAO appDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private TemplatedMailService templatedMailService;
    @Autowired private CloudOsConfiguration configuration;

    private TwoFactorAuthService getTwoFactorAuthService() { return configuration.getTwoFactorAuthService(); }

    @GET
    public Response findAll (@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can list all accounts
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        return Response.ok(accountDAO.findAll()).build();
    }

    @PUT
    @Path("/{name}")
    public Response addAccount(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                               @PathParam("name") String accountUuid,
                               @Valid AccountRequest request) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can create new accounts
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        if (request.isTwoFactor()) {
            request.setAuthIdInt(getTwoFactorAuthService().addUser(request.getEmail(), request.getMobilePhone(), request.getMobilePhoneCountryCodeString()));
        }

        final Account created;
        try {
            created = accountDAO.create(request);
        } catch (Exception e) {
            log.error("addAccount: error creating admin: "+e, e);
            return Response.serverError().build();
        }

        sendInvitation(admin, created, request.getPassword());

        return Response.ok(created).build();
    }

    public void sendInvitation(Account admin, Account created, String password) {
        // todo: use the event bus for this?
        // Send welcome email with password and link to login and change it
        final String hostname = configuration.getHostname();
        final TemplatedMail mail = new TemplatedMail()
                .setTemplateName(TemplatedMailService.T_WELCOME)
                .setLocale("en_US") // todo: set this at first-time-setup
                .setFromEmail(admin.getName() + "@" + hostname)
                .setToEmail(created.getEmail())
                .setToName(created.getName())
                .setParameter(TemplatedMailService.PARAM_ACCOUNT, created)
                .setParameter(TemplatedMailService.PARAM_ADMIN, admin)
                .setParameter(TemplatedMailService.PARAM_HOSTNAME, hostname)
                .setParameter(TemplatedMailService.PARAM_PASSWORD, password);
        try {
            templatedMailService.getMailSender().deliverMessage(mail);
        } catch (Exception e) {
            log.error("addAccount: error sending welcome email: "+e, e);
        }
    }

    @POST
    public Response login(@Valid AccountLoginRequest login) {

        long start = System.currentTimeMillis();
        try {
            final Account account;
            if (login.isSecondFactor()) {
                account = accountDAO.findByName(login.getName());
                if (account == null) return ResourceUtil.notFound();
                try {
                    getTwoFactorAuthService().verify(account.getAuthIdInt(), login.getSecondFactor());
                } catch (Exception e) {
                    return ResourceUtil.invalid();
                }

            } else {
                try {
                    account = accountDAO.authenticate(login);
                } catch (AuthenticationException e) {
                    log.warn("Error authenticating: " + e);
                    switch (e.getProblem()) {
                        case NOT_FOUND:
                            return ResourceUtil.notFound();
                        case INVALID:
                            return ResourceUtil.forbidden();
                        case BOOTCONFIG_ERROR:
                        default:
                            return serverError();
                    }
                }

                // check for 2-factor
                if (account.isTwoFactor()) {
                    // if a device was supplied, check to see that most recent auth-time for that device
                    if (!deviceIsAuthorized(account, login.getDeviceId())) {
                        return Response.ok(AuthResponse.TWO_FACTOR).build();
                    }
                }
            }

            // authenticate above should have returned 403 when the password didn't match, since
            // when an account is suspended its kerberos password is changed to a long random string.
            // ...but just in case...
            if (account.isSuspended()) return ResourceUtil.forbidden();

            // update last login
            account.setLastLogin();
            accountDAO.update(account);

            // if this was a 2-factor success and a deviceId was given, update the device auth time
            if (login.isSecondFactor() && login.hasDevice()) {
                updateDeviceAuth(account, login.getDeviceId(), login.getDeviceName());
            }

            account.setPassword(login.getPassword()); // keep the password in the session
            final String sessionId = sessionDAO.create(account);

            // set apps
            account.setAvailableApps(new ArrayList<>(appDAO.getAvailableAppDetails().values()));

            // the password will be scrubbed from the json response
            return Response.ok(new AuthResponse(sessionId, account)).build();

        } catch (Exception e) {
            log.error("Error logging in account: "+e, e);
            return Response.serverError().build();

        } finally {
            log.info("login executed in "+ TimeUtil.formatDurationFrom(start));
        }
    }

    private void updateDeviceAuth(Account account, String deviceId, String deviceName) {
        if (StringUtil.empty(deviceId)) return;
        final AccountDevice accountDevice = deviceDAO.findByAccountAndDevice(account.getAccountName(), deviceId);
        if (accountDevice == null) {
            deviceDAO.create(new AccountDevice()
                    .setAccount(account.getAccountName())
                    .setDeviceId(deviceId)
                    .setDeviceName(deviceName)
                    .setAuthTime());
        } else {
            deviceDAO.update(accountDevice.setDeviceName(deviceName).setAuthTime());
        }
    }

    public static final long DEVICE_TIMEOUT = TimeUnit.DAYS.toMillis(30);

    private boolean deviceIsAuthorized(Account account, String deviceId) {
        if (StringUtil.empty(deviceId)) return false;
        final AccountDevice accountDevice = deviceDAO.findByAccountAndDevice(account.getAccountName(), deviceId);
        return accountDevice != null && accountDevice.isAuthYoungerThan(DEVICE_TIMEOUT);
    }

    @POST
    @Path("/{name}")
    public Response update (@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                            @PathParam("name") String name,
                            @Valid AccountRequest request) {

        final Account account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.notFound(apiKey);

        // admins can update anyone, others can only update themselves
        if (!account.isAdmin() && !account.getName().equals(name)) {
            return ResourceUtil.forbidden();
        }

        // if the caller is not an admin, ensure the request is not an admin
        if (!account.isAdmin()) request.setAdmin(false);

        // if caller is an admin suspending a user, ensure they are not suspending themselves
        if (account.isAdmin() && request.isSameName(account) && request.isSuspended()) {
            return ResourceUtil.invalid("err.admin.cannotSuspendSelf");
        }

        Account toUpdate = new Account(request);
        toUpdate.setName(name); // force account name that was in path
        try {
            toUpdate = accountDAO.update(toUpdate);
        } catch (Exception e) {
            log.error("Error calling AccountDAO.save: "+e, e);
            return serverError();
        }

        if (request.isSuspended()) {
            sessionDAO.invalidateAllSessions(toUpdate.getUuid());
        } else {
            sessionDAO.update(apiKey, account);
        }

        return Response.ok(account).build();
    }

    @POST
    @Path("/{name}/password")
    public Response changePassword(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                   @PathParam("name") String name,
                                   @Valid PasswordChangeRequest request) {

        final Account account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.notFound(apiKey);

        // non-admins cannot change anyone's password but their own
        final boolean targetIsSelf = account.getName().equals(name);
        if ( !( account.isAdmin() || targetIsSelf) ) return ResourceUtil.forbidden();

        final Account target = targetIsSelf ? account : accountDAO.findByName(name);

        try {
            if (account.isAdmin()) {
                accountDAO.adminChangePassword(name, request.getNewPassword());
                if (request.isSendInvite()) {
                    sendInvitation(account, target, request.getNewPassword());
                }
            } else {
                accountDAO.changePassword(account.getName(), request.getOldPassword(), request.getNewPassword());
            }

        } catch (AuthenticationException e) {
            return ResourceUtil.forbidden();

        } catch (Exception e) {
            log.error("Error calling AccountDAO.changePassword: "+e, e);
            return serverError();
        }

        return Response.ok(account).build();
    }

    @GET
    @Path("/{name}")
    public Response find(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                         @PathParam("name") String name) {
        long start = System.currentTimeMillis();
        name = name.toLowerCase();

        final Account account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.notFound(apiKey);

        // non-admins are only allowed to lookup their own account
        if (!account.isAdmin() && !account.getName().equals(name)) return ResourceUtil.forbidden();

        try {
            final Account found = accountDAO.findByName(name);
            if (found == null) return ResourceUtil.notFound(name);

            if (found.isSuspended() && !account.isAdmin()) {
                // suspended accounts cannot be looked up, except by admins
                return ResourceUtil.notFound(name);
            }

            return Response.ok(found).build();

        } catch (Exception e) {
            log.error("Error looking up account: "+e, e);
            return Response.serverError().build();

        } finally {
            log.info("find executed in "+ TimeUtil.formatDurationFrom(start));
        }
    }

    @DELETE
    @Path("/{name}")
    public Response delete(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                           @PathParam("name") String name) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        name = name.toLowerCase();

        // only admins can delete accounts
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        // cannot delete your own account or system mailer account
        if (name.equals(admin.getName()) || name.equals(configuration.getSmtpMailConfig().getUser())) {
            return ResourceUtil.forbidden();
        }

        Account toDelete = accountDAO.findByName(name);
        if (toDelete == null) return ResourceUtil.notFound(name);
        if (toDelete.hasAuthId()) {
            getTwoFactorAuthService().deleteUser(toDelete.getAuthIdInt());
        }

        try {
            accountDAO.delete(name);
        } catch (Exception e) {
            log.error("delete: error deleting account "+name+": "+e, e);
            return Response.serverError().build();
        }

        return Response.ok(Boolean.TRUE).build();
    }
}
