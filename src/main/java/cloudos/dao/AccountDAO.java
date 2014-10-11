package cloudos.dao;

import cloudos.model.Account;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountRequest;
import cloudos.model.auth.AuthenticationException;
import cloudos.resources.ApiConstants;
import cloudos.service.KerberosService;
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

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@Repository  @Slf4j
public class AccountDAO extends AccountBaseDAO<Account> {

    @Autowired private KerberosService kerberos;
    @Autowired private RootyService rooty;

    @Override public Account authenticate(LoginRequest loginRequest) throws AuthenticationException {

        final String accountName = loginRequest.getName();
        final String password = loginRequest.getPassword();

        kerberos.authenticate(accountName, password);

        final Account account = findByName(accountName);
        if (account == null) throw new AuthenticationException(AuthenticationException.Problem.NOT_FOUND);

        return account;
    }

    public void changePassword(Account account, String oldPassword, String newPassword) throws AuthenticationException {
        kerberos.changePassword(account.getAccountName(), oldPassword, newPassword);
        account.getHashedPassword().setResetToken(null);
        update(account);
    }

    @Override
    public void setPassword(Account account, String newPassword) {
        kerberos.adminChangePassword(account.getAccountName(), newPassword);
        account.getHashedPassword().setResetToken(null);
        update(account);
    }

    public List<Account> findAccounts() {
        final List<Account> accounts = findAll();
        Collections.sort(accounts, Account.SORT_ACCOUNT_NAME);
        return accounts;
    }

    public Account create(AccountRequest request) throws Exception {

        if (!request.hasPassword()) request.setPassword(ApiConstants.randomPassword());

        final Account account = new Account().populate(request);

        // ignored for cloudos since kerberos is used, but must not be null
        account.setHashedPassword(new HashedPassword(RandomStringUtils.randomAlphanumeric(20)));

        // generate an email verification code for new accounts
        account.initEmailVerificationCode();

        final CommandResult result = kerberos.createPrincipal(request);

        // Create account in DB
        try {
            super.create(account);
        } catch (Exception e) {
            final String message = "create: principal created in kerberos but account not persisted to DB! " + e;
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }

        // Tell the rooty subsystems we have a new account
        final AccountEvent event = new NewAccountEvent()
                .setName(account.getName())
                .setAdmin(account.isAdmin());
        rooty.getSender().write(event);

        log.info("create: result="+result);
        return account;
    }

    @Override
    public Account update(@Valid Account account) {

        final Account existing = findByName(account.getName());
        boolean isSuspending = !existing.isSuspended() && account.isSuspended();
        existing.populate(account);
        if (isSuspending) {
            kerberos.adminChangePassword(account.getName(), RandomStringUtils.randomAlphanumeric(20));
        }

        return super.update(existing);
    }

    public void delete(String accountName) {

        final Account account = findByName(accountName);
        if (account == null) {
            log.warn("delete: account not found (silently returning): "+accountName);
            return;
        }

        kerberos.deletePrincipal(accountName);
        try {
            super.delete(account.getUuid());
        } catch (Exception e) {
            final String message = "delete: principal deleted in kerberos but account not deleted in storageEngine! " + e;
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }

        // Tell the rooty subsystems we have removed an account
        final AccountEvent event = new RemoveAccountEvent()
                .setName(account.getName())
                .setAdmin(account.isAdmin());
        rooty.getSender().write(event);
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

}
