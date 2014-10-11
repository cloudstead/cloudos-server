package cloudos.resources;

import cloudos.dao.AccountDAO;
import cloudos.dao.AppDAO;
import cloudos.model.Account;
import cloudos.model.AccountBase;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.ChangePasswordRequest;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountRequest;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static cloudos.resources.ApiConstants.ACCOUNTS_ENDPOINT;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ACCOUNTS_ENDPOINT)
@Service @Slf4j
public class AccountsResource extends AccountsResourceBase<Account, CloudOsAuthResponse> {

    public static final String PARAM_NAME = "name";
    public static final String EP_CHANGE_PASSWORD = "/{"+PARAM_NAME+"}/password";
    public static String getChangePasswordPath(String name) { return ACCOUNTS_ENDPOINT + EP_CHANGE_PASSWORD.replace("{"+PARAM_NAME+"}", name); }

    @Override protected void afterSuccessfulLogin(LoginRequest login, Account account) throws Exception {
        // keep the password in the session, it'll be scrubbed from the json response
        account.setPassword(login.getPassword());

        // set apps
        account.setAvailableApps(new ArrayList<>(appDAO.getAvailableAppDetails().values()));
    }

    @Override
    protected CloudOsAuthResponse buildAuthResponse(String sessionId, Account account) {
        return new CloudOsAuthResponse(sessionId, account);
    }

    private Response serverError() { return Response.serverError().build(); }

    @Autowired private AccountDAO accountDAO;
    @Autowired private AppDAO appDAO;
    @Autowired private TemplatedMailService mailService;
    @Autowired private CloudOsConfiguration configuration;

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

        if (request.isTwoFactor()) set2factor(request);

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
                .setFromName(admin.getFullName())
                .setToEmail(created.getEmail())
                .setToName(created.getName())
                .setParameter(TemplatedMailService.PARAM_ACCOUNT, created)
                .setParameter(TemplatedMailService.PARAM_ADMIN, admin)
                .setParameter(TemplatedMailService.PARAM_HOSTNAME, hostname)
                .setParameter(TemplatedMailService.PARAM_PASSWORD, password);
        try {
            mailService.getMailSender().deliverMessage(mail);
        } catch (Exception e) {
            log.error("addAccount: error sending welcome email: "+e, e);
        }
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

        final Account found = accountDAO.findByName(name);
        if (found == null) return ResourceUtil.notFound(name);

        if (!request.isTwoFactor() && found.isTwoFactor()) {
            // they are turning off two-factor auth
            remove2factor(found);
        } else if (request.isTwoFactor() && !found.isTwoFactor()) {
            // they are turning on two-factor auth
            set2factor(request);
        } else if (!request.getMobilePhone().equals(found.getMobilePhone())) {
            // they changed their phone number, remove old auth id and add a new one
            remove2factor(found);
            set2factor(request);
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

    private AccountBase set2factor(AccountRequest request) {
        return request.setAuthIdInt(getTwoFactorAuthService().addUser(request.getEmail(), request.getMobilePhone(), request.getMobilePhoneCountryCodeString()));
    }

    private void remove2factor(Account account) {
        getTwoFactorAuthService().deleteUser(account.getAuthIdInt());
    }

    @POST
    @Path(EP_CHANGE_PASSWORD)
    public Response changePassword(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                   @PathParam(PARAM_NAME) String name,
                                   @Valid ChangePasswordRequest request) {

        final Account account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.notFound(apiKey);

        // non-admins cannot change anyone's password but their own
        final boolean targetIsSelf = account.getName().equals(name);
        if ( !( account.isAdmin() || targetIsSelf) ) return ResourceUtil.forbidden();

        final Account target = accountDAO.findByName(name);

        try {
            if (account.isAdmin()) {
                accountDAO.setPassword(target, request.getNewPassword());
                if (request.isSendInvite()) {
                    sendInvitation(account, target, request.getNewPassword());
                }
            } else {
                accountDAO.changePassword(target, request.getOldPassword(), request.getNewPassword());
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
        if (toDelete.hasAuthId()) remove2factor(toDelete);

        try {
            accountDAO.delete(name);
        } catch (Exception e) {
            log.error("delete: error deleting account "+name+": "+e, e);
            return Response.serverError().build();
        }

        return Response.ok(Boolean.TRUE).build();
    }

}
