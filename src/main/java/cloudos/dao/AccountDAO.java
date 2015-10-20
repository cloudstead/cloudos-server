package cloudos.dao;

import cloudos.appstore.model.AppRuntime;
import cloudos.model.Account;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountRequest;
import cloudos.resources.ApiConstants;
import cloudos.service.CloudOsLdapService;
import cloudos.service.RootyService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractLdapDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import rooty.events.account.AccountEvent;
import rooty.events.account.NewAccountEvent;
import rooty.events.account.RemoveAccountEvent;
import rooty.toots.app.AppScriptMessage;
import rooty.toots.app.AppScriptMessageType;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

@Repository  @Slf4j
public class AccountDAO extends AbstractLdapDAO<Account> implements BasicAccountDAO<Account> {

    @Autowired private RootyService rooty;
    @Autowired private AppDAO appDAO;
    @Autowired private AccountGroupDAO groupDAO;
    @Getter @Autowired private CloudOsLdapService ldapService;

    public Account authenticate(LoginRequest loginRequest) throws AuthenticationException {

        final String accountName = loginRequest.getName();
        final String password = loginRequest.getPassword();

        try {
            authenticate(accountName, password);
        } catch (Exception e) {
            log.error("authenticate: "+e, e);
            throw new AuthenticationException(AuthenticationException.Problem.INVALID);
        }

        final Account account = findByName(accountName);
        if (account == null) throw new AuthenticationException(AuthenticationException.Problem.NOT_FOUND);

        return account;
    }

    public void changePassword(Account account, String oldPassword, String newPassword) throws AuthenticationException {
        checkAuth(account, oldPassword);
        ldapService.changePassword(account.getName(), oldPassword, newPassword);

        // Tell the rooty subsystems we've changed the password
        broadcastPasswordChange(account, newPassword);
    }

    public void checkAuth(Account account, String oldPassword) throws AuthenticationException {
        try {
            authenticate(account.getName(), oldPassword);
        } catch (Exception e) {
            final String message = "changePassword: Error authenticating with current password: " + e;
            log.error(message, e);
            throw new AuthenticationException(AuthenticationException.Problem.INVALID);
        }
    }

    public void setPassword(Account account, String newPassword) {
        ldapService.adminChangePassword(account.getName(), newPassword);
        account.clearResetPasswordToken();
        account.setPassword(null);
        update(account);

        // Tell the rooty subsystems we've changed the password
        broadcastPasswordChange(account, newPassword);
    }

    @Override public Account postCreate(Account account, Object context) {
        groupDAO.addToDefaultGroup(account);
        if (account.isAdmin()) groupDAO.addToAdminGroup(account);
        return super.postCreate(account, context);
    }

    public Account create(AccountRequest request) throws Exception {

        if (!request.hasPassword()) request.setPassword(ApiConstants.randomPassword());

        final Account account = (Account) new Account(config()).merge(request).clean();

        // generate an email verification code for new accounts
        account.initEmailVerificationCode();

        // Create account in LDAP
        try {
            super.create(account);
        } catch (Exception e) {
            final String message = "create: error creating account in LDAP: " + e;
            log.error(message, e);
            die(message, e);
        }

        // Tell the rooty subsystems we have a new account
        final AccountEvent event = new NewAccountEvent()
                .setName(account.getName())
                .setAdmin(account.isAdmin());
        rooty.getSender().write(event);

        broadcastNewAccount(account);

        return account;
    }

    @Override public Account update(@Valid Account account) {

        final Account existing = findByDn(account.getDn());
        if (existing == null) die("Cannot update non-existent account: "+account.getDn());

        boolean isSuspending = !existing.isSuspended() && account.isSuspended();
        existing.merge(account);
        if (isSuspending) {
            final String newPassword = randomAlphanumeric(20);
            existing.setPassword(newPassword);
            ldapService.adminChangePassword(account.getName(), newPassword);
        }

        return super.update(existing);
    }

    public void delete(String accountName) {

        final Account account = findByName(accountName);
        if (account == null) {
            log.warn("delete: account not found (silently returning): "+accountName);
            return;
        }

        try {
            super.delete(account.getName());
        } catch (Exception e) {
            final String message = "delete: account deleted in LDAP but account not deleted in storageEngine! " + e;
            log.error(message, e);
            die(message, e);
        }

        // Tell the rooty subsystems we have removed an account
        final AccountEvent event = new RemoveAccountEvent()
                .setName(account.getName())
                .setAdmin(account.isAdmin());
        rooty.getSender().write(event);

        broadcastDeleteAccount(account);
    }

    private void broadcastNewAccount (Account account) {
        final Map<String, AppRuntime> apps = appDAO.getAvailableRuntimes();

        for (Map.Entry<String, AppRuntime> app : apps.entrySet()) {
            final AppRuntime runtime = app.getValue();
            if (runtime.hasUserManagement() && runtime.getAuthentication().getUser_management().hasUserCreate()) {
                final AppScriptMessage message = new AppScriptMessage()
                        .setApp(runtime.getDetails().getName())
                        .setType(AppScriptMessageType.user_create)
                        .addArg(account.getName());
                rooty.getSender().write(message);
            }
        }
    }

    private void broadcastDeleteAccount (Account account) {
        final Map<String, AppRuntime> apps = appDAO.getAvailableRuntimes();

        for (Map.Entry<String, AppRuntime> app : apps.entrySet()) {
            final AppRuntime runtime = app.getValue();
            if (runtime.hasUserManagement() && runtime.getAuthentication().getUser_management().hasUserDelete()) {
                final AppScriptMessage message = new AppScriptMessage()
                        .setApp(runtime.getDetails().getName())
                        .setType(AppScriptMessageType.user_delete)
                        .addArg(account.getName());
                rooty.getSender().write(message);
            }
        }
    }

    private void broadcastPasswordChange(Account account, String newPassword) {
        final Map<String, AppRuntime> apps = appDAO.getAvailableRuntimes();

        for (Map.Entry<String, AppRuntime> app : apps.entrySet()) {
            final AppRuntime runtime = app.getValue();
            if (runtime.hasUserManagement() && runtime.getAuthentication().getUser_management().hasChangePassword()) {
                final AppScriptMessage message = new AppScriptMessage()
                        .setApp(runtime.getDetails().getName())
                        .setType(AppScriptMessageType.user_change_password)
                        .addArg(account.getName())
                        .addArg(newPassword);
                rooty.getSender().write(message);
            }
        }
    }

    public List<Account> findAdmins() { return findByField(Account.STATUS, Account.Status.admins.name()); }

    @Override public Account findByActivationKey(String key) {
        return findByUniqueField(Account.EMAIL_VERIFICATION_CODE, key);
    }

    @Override public Account findByResetPasswordToken(String token) {
        return findByUniqueField(Account.RESET_PASSWORD_TOKEN, token);
    }

    @Override protected String formatBound(String bound, String value) {
        if (bound.equals(Account.STATUS)) {
            switch (Account.Status.valueOf(value)) {
                case active: return "(&("+config().getUser_lastLogin()+"=*)("+config().getUser_suspended()+"=false))";
                case invited: return "(&(!("+config().getUser_lastLogin()+"=*))("+config().getUser_suspended()+"=false))";
                case suspended: return "("+config().getUser_suspended()+"=true)";
                case admins: return "(&("+config().getUser_admin()+"=true)("+config().getUser_suspended()+"=false))";
                case non_admins: return "("+config().getUser_admin()+"=false)";
                default: return die("formatBound: invalid bound=value (" + bound + "=" + value + ")");
            }

        } else if (bound.equals(idField())) {
            return "(" + idField() + "=" + value + ")";

        } else if (bound.equals("resetPasswordToken")) {
            return "("+bound+"="+value+")";

        } else {
            return notSupported("formatBound: " + bound);
        }
    }
}
