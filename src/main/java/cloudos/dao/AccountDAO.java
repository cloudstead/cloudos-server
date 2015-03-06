package cloudos.dao;

import cloudos.appstore.model.AppRuntime;
import cloudos.model.Account;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountRequest;
import cloudos.resources.ApiConstants;
import cloudos.service.KerberosService;
import cloudos.service.LdapService;
import cloudos.service.RootyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.wizard.model.HashedPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import rooty.events.account.AccountEvent;
import rooty.events.account.NewAccountEvent;
import rooty.events.account.RemoveAccountEvent;
import rooty.toots.app.AppScriptMessage;
import rooty.toots.app.AppScriptMessageType;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Repository  @Slf4j
public class AccountDAO extends AccountBaseDAO<Account> {

    @Autowired private LdapService ldap;
    @Autowired private KerberosService kerberos;
    @Autowired private RootyService rooty;
    @Autowired private AppDAO appDAO;
    @Autowired private AccountGroupDAO groupDAO;
    @Autowired private AccountGroupMemberDAO memberDAO;

    @Override public Account authenticate(LoginRequest loginRequest) throws AuthenticationException {

        final String accountName = loginRequest.getName();
        final String password = loginRequest.getPassword();

        kerberos.authenticate(accountName, password);

        final Account account = findByName(accountName);
        if (account == null) throw new AuthenticationException(AuthenticationException.Problem.NOT_FOUND);

        return account;
    }

    public void changePassword(Account account, String oldPassword, String newPassword) throws AuthenticationException {
        checkAuth(account, oldPassword);
        ldap.changePassword(account.getAccountName(), oldPassword, newPassword);
        kerberos.changePassword(account.getAccountName(), oldPassword, newPassword);
        account.getHashedPassword().setResetToken(null);
        update(account);

        // Tell the rooty subsystems we've changed the password
        broadcastPasswordChange(account, newPassword);
    }

    public void checkAuth(Account account, String oldPassword) throws AuthenticationException {
        try {
            kerberos.authenticate(account.getName(), oldPassword);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            final String message = "changePassword: Error authenticating with current password: " + e;
            log.error(message, e);
            die(message, e);
        }
    }

    @Override
    public void setPassword(Account account, String newPassword) {
        ldap.adminChangePassword(account.getAccountName(), newPassword);
        kerberos.adminChangePassword(account.getAccountName(), newPassword);
        account.getHashedPassword().setResetToken(null);
        update(account);

        // Tell the rooty subsystems we've changed the password
        broadcastPasswordChange(account, newPassword);
    }

    public List<Account> findAccounts() {
        final List<Account> accounts = findAll();
        Collections.sort(accounts, Account.SORT_ACCOUNT_NAME);
        return accounts;
    }

    @Override
    public Account postCreate(Account account, Object context) {
        groupDAO.addToDefaultGroup(account);
        if (account.isAdmin()) groupDAO.addToAdminGroup(account);
        return super.postCreate(account, context);
    }

    public Account create(AccountRequest request) throws Exception {

        if (!request.hasPassword()) request.setPassword(ApiConstants.randomPassword());

        final Account account = new Account().populate(request);

        // ignored for cloudos since kerberos is used, but must not be null
        account.setHashedPassword(new HashedPassword(RandomStringUtils.randomAlphanumeric(20)));

        // generate an email verification code for new accounts
        account.initEmailVerificationCode();

        final CommandResult ldapResult = ldap.createUser(request);
        final CommandResult kerberosResult = kerberos.createPrincipal(request);

        // Create account in DB
        try {
            super.create(account);
        } catch (Exception e) {
            final String message = "create: account created in ldap / kerberos but account not persisted to DB! " + e;
            log.error(message, e);
            die(message, e);
        }

        // Tell the rooty subsystems we have a new account
        final AccountEvent event = new NewAccountEvent()
                .setName(account.getName())
                .setAdmin(account.isAdmin());
        rooty.getSender().write(event);

        broadcastNewAccount(account);

        log.info("create: ldap result="+ldapResult);
        log.info("create: krb result="+kerberosResult);
        return account;
    }

    @Override
    public Account update(@Valid Account account) {

        final Account existing = findByName(account.getName());
        boolean isSuspending = !existing.isSuspended() && account.isSuspended();
        existing.populate(account);
        if (isSuspending) {
            ldap.adminChangePassword(account.getName(), RandomStringUtils.randomAlphanumeric(20));
            kerberos.adminChangePassword(account.getName(), RandomStringUtils.randomAlphanumeric(20));
        }

        return super.update(existing);
    }

    public void delete(String accountName) {

        try {
            ldap.deleteUser(accountName);
        } catch (Exception e) {
            log.warn("delete: Error calling ldap.deleteUser("+accountName+"): "+e, e);
        }

        final Account account = findByName(accountName);
        if (account == null) {
            log.warn("delete: account not found (silently returning): "+accountName);
            return;
        }

        try {
            super.delete(account.getUuid());
        } catch (Exception e) {
            final String message = "delete: account deleted in ldap / kerberos but account not deleted in storageEngine! " + e;
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

    @Override
    protected String formatBound(String entityAlias, String bound, String value) {
        return "("+innerBound(entityAlias, bound, value)+")";
    }

    protected String innerBound(String entityAlias, String bound, String value) {
        switch (bound) {
            case Account.STATUS:
                switch (Account.Status.valueOf(value)) {
                    case active: return entityAlias+".lastLogin IS NOT NULL AND "+entityAlias+".suspended = false";
                    case invited: return entityAlias+".lastLogin IS NULL AND "+entityAlias+".suspended = false";
                    case suspended: return entityAlias+".suspended = true";
                    case admins: return entityAlias+".admin = true AND "+entityAlias+".suspended = false";
                    case non_admins: return entityAlias+".admin = false";
                    default: throw new IllegalArgumentException("Invalid status bound: "+value);
                }

            default:
                throw new IllegalArgumentException("Invalid bound: "+bound);
        }
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

}
