package cloudos.resources;

import cloudos.model.Account;
import cloudos.model.auth.*;
import cloudos.model.support.AccountRequest;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cloudos.resources.ApiConstants.ACCOUNTS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;

@Slf4j
public class AccountsResourceTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Account Management";

    @Before public void resetTokens() { flushTokens(); }

    public AuthResponse assertAccount(AccountRequest request) throws Exception {
        return assertAccount(request, null);
    }

    public AuthResponse assertAccount(AccountRequest request, String deviceId) throws Exception {

        final String accountName = request.getName();

        // be admin for this part
        pushToken(adminToken);
        apiDocs.addNote("creating a regular account: " + accountName);
        RestResponse response = put(ACCOUNTS_ENDPOINT + "/" + accountName, toJson(request));
        final Account account = fromJson(response.json, Account.class);
        assertNotNull(account);
        assertEquals(request.getEmail(), account.getEmail());

        // grab password from email
        final MockTemplatedMailSender sender = getTemplatedMailSender();
        assertEquals(1, sender.messageCount());
        final String password = sender.first().getParameters().get(TemplatedMailService.PARAM_PASSWORD).toString();
        sender.reset();

        // todo: check password against LDAP
        // ensure password is correct in LDAP
        // assertEquals(password, getKerberos().getPassword(account.getName()));

        // remove admin token from api, we'll login as a regular account here
        popToken();

        // login with password found in email
        response = login(accountName, password, deviceId);
        assertEquals(200, response.status);

        AuthResponse authResponse = fromJson(response.json, CloudOsAuthResponse.class);
        assertNotNull(authResponse.getSessionId());

        if (authResponse.isTwoFactor()) {
            final RestResponse secondFactorResponse = secondFactor(accountName, "0000000", deviceId);
            assertEquals(200, secondFactorResponse.status);
            authResponse = fromJson(secondFactorResponse.json, CloudOsAuthResponse.class);
            assertNotNull(authResponse.getSessionId());
        }

        return authResponse;
    }

    public RestResponse login(String accountName, String password) throws Exception {
        final LoginRequest loginRequest = new LoginRequest().setName(accountName).setPassword(password);
        apiDocs.addNote("login account " + accountName);
        return login(loginRequest);
    }

    public RestResponse login(String accountName, String password, String deviceId) throws Exception {
        final LoginRequest loginRequest = new LoginRequest()
                .setName(accountName)
                .setPassword(password)
                .setDeviceId(deviceId)
                .setDeviceName(deviceId);
        apiDocs.addNote("login account " + accountName + " with device " + deviceId);
        return login(loginRequest);
    }

    public RestResponse secondFactor(String accountName, String secondFactor, String deviceId) throws Exception {

        apiDocs.addNote("account requires 2-factor auth. verify that a request to view profile should fail, since login has not been completed");
        assertEquals(404, doGet(ACCOUNTS_ENDPOINT + "/" + accountName).status);

        final LoginRequest loginRequest = new LoginRequest()
                .setName(accountName)
                .setSecondFactor(secondFactor)
                .setDeviceId(deviceId)
                .setDeviceName(deviceId);
        apiDocs.addNote("send 2-factor verification token for account " + accountName);
        return login(loginRequest);
    }

    @Test public void testListAllUsers () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "list all accounts");
        pushToken(adminToken);
        apiDocs.addNote("fetch all accounts");
        final Account[] accounts = fromJson(doGet(ACCOUNTS_ENDPOINT).json, Account[].class);
        assertTrue(accounts.length > 0);
    }

    @Test public void testCreateAccountWithSystemPassword () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create account with system password");
        final String accountName = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(ldap(), accountName);
        assertAccount(request);
    }

    @Test public void testCreateAccountWithAdminSuppliedPassword () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create account with admin-supplied password");
        final String accountName = randomAlphanumeric(10);
        final String password = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(ldap(), accountName, password, false);
        assertAccount(request);
    }

    @Test public void testSuspendAccount () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "suspend a account, verify their session is invalidated and they can not login");
        final String accountName = randomAlphanumeric(10).toLowerCase();
        final String password = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(ldap(), accountName, password, false);
        assertAccount(request);

        apiDocs.addNote("request to view profile should succeed, since account has not yet been suspended");
        assertEquals(200, get(ACCOUNTS_ENDPOINT + "/" + accountName).status);

        // do this as admin
        apiDocs.addNote("switch API token to admin account, suspend the account just created");
        suspend(request);
        RestResponse response;

        apiDocs.addNote("request to view profile should fail, since session has been invalidated (not found)");
        response = doGet(ACCOUNTS_ENDPOINT + "/" + accountName);
        assertEquals(404, response.status);

        // login will fail with old password
        response = login(accountName, password);
        assertEquals(404, response.status);
    }

    @Test public void testCreateAccountWith2FactorAuth () throws Exception {
        if (empty(getConfiguration().getAuthy().getUser())) {
            log.warn("testCreateAccountWith2FactorAuth: No auth config found, skipping test");
            return;
        }
        apiDocs.startRecording(DOC_TARGET, "create a account with 2-factor authentication and login");
        final String accountName = randomAlphanumeric(10);
        final String password = randomAlphanumeric(10);
        final String device1 = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(ldap(), accountName, password, false);
        request.setTwoFactor(true);
        final AuthResponse authResponse = assertAccount(request, device1);
        assertNotEquals(AuthResponse.TWO_FACTOR_SID, authResponse.getSessionId());

        apiDocs.addNote("request to view profile should now succeed");
        assertEquals(200, get(ACCOUNTS_ENDPOINT + "/" + accountName).status);

        RestResponse login;

        flushTokens();
        apiDocs.addNote("login again, should not require 2-factor auth since we just supplied it");
        login = login(accountName, password, device1);
        assertEquals(200, login.status);
        assertNotEquals(AuthResponse.TWO_FACTOR_SID, fromJson(login.json, CloudOsAuthResponse.class).getSessionId());

        flushTokens();
        apiDocs.addNote("login from a different device, should require 2-factor auth for new device");
        login = login(accountName, password, device1+"_different");
        assertEquals(200, login.status);
        assertEquals(AuthResponse.TWO_FACTOR_SID, fromJson(login.json, CloudOsAuthResponse.class).getSessionId());
    }

    @Test public void testAdminResetPassword () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "as an admin, change another account's password");

        RestResponse response;

        apiDocs.addNote("add a regular account");
        final String accountName = randomAlphanumeric(10).toLowerCase();
        final String password = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(ldap(), accountName, password, false);
        final AuthResponse authResponse = assertAccount(request);

        apiDocs.addNote("add a second account");
        final String secondAccountName = randomAlphanumeric(10).toLowerCase();
        final AuthResponse secondAuth = assertAccount(newAccountRequest(ldap(), secondAccountName));

        apiDocs.addNote("as second account, try to change first account's password using admin endpoint, this will fail");
        final String newPassword = randomAlphanumeric(10);
        final ChangePasswordRequest passwordChangeRequest = new ChangePasswordRequest("-", newPassword);
        flushTokens(); pushToken(secondAuth.getSessionId());
        response = doPost(ACCOUNTS_ENDPOINT + "/" + accountName + "/password", toJson(passwordChangeRequest));
        assertEquals(403, response.status);

        apiDocs.addNote("ensure first account can still login with original password");
        flushTokens();
        assertEquals(200, login(accountName, password).status);

        apiDocs.addNote("as admin, change first account's password");
        flushTokens(); pushToken(adminToken);
        doPost(ACCOUNTS_ENDPOINT + "/" + accountName + "/password", toJson(passwordChangeRequest));

        apiDocs.addNote("ensure original password no longer works");
        flushTokens();
        assertEquals(404, login(accountName, password).status);

        apiDocs.addNote("ensure first account can now login with new password");
        flushTokens();
        assertEquals(200, login(accountName, newPassword).status);
    }

    @Test public void testChangePassword () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "exercise the change-password workflow");

        final String accountName = randomAlphanumeric(10).toLowerCase();
        final String password = randomAlphanumeric(10);

        apiDocs.addNote("add a regular account");
        final AccountRequest request = newAccountRequest(ldap(), accountName, password, false);
        final AuthResponse authResponse = assertAccount(request);
        final Account account = (Account) authResponse.getAccount();
        pushToken(authResponse.getSessionId());

        apiDocs.addNote("login with current password, should work");
        fullLogin(accountName, password, null);

        apiDocs.addNote("change password");
        final String newPassword = password + "_changed";
        final ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest()
                .setUuid(account.getUuid())
                .setOldPassword(password)
                .setNewPassword(newPassword);
        post(AccountsResource.getChangePasswordPath(account.getName()), toJson(changePasswordRequest));

        expectFailedLogin(accountName, password);

        apiDocs.addNote("login with new password, should work");
        fullLogin(accountName, newPassword, null);
    }

    @Test public void testForgotPassword () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "exercise the forgot-password workflow");

        final String accountName = randomAlphanumeric(10).toLowerCase();
        final String password = randomAlphanumeric(10);

        apiDocs.addNote("add a regular account");
        final AccountRequest request = newAccountRequest(ldap(), accountName, password, false);
        final AuthResponse authResponse = assertAccount(request);
        pushToken(authResponse.getSessionId());

        final MockTemplatedMailSender sender = getTemplatedMailSender();
        sender.reset();
        flushTokens();
        apiDocs.addNote("hit forgot password link");
        post(ApiConstants.AUTH_ENDPOINT + AuthResource.EP_FORGOT_PASSWORD, accountName);

        assertEquals(1, sender.getMessages().size());
        final String url = sender.getFirstMessage().getParameters().get(AuthResource.PARAM_RESETPASSWORD_URL).toString();
        assertNotNull(url);
        final Matcher matcher = Pattern.compile("^https?://[^\\?]+\\?key=(\\w+)").matcher(url);
        assertTrue(matcher.find());
        final String token = matcher.group(1);

        apiDocs.addNote("hit reset password link");
        final String newPassword = password+"_changed";
        final ResetPasswordRequest resetRequest = new ResetPasswordRequest().setToken(token).setPassword(newPassword);
        post(ApiConstants.AUTH_ENDPOINT + AuthResource.EP_RESET_PASSWORD, toJson(resetRequest));

        expectFailedLogin(accountName, password);

        apiDocs.addNote("login with new password - should succeed but should still require 2-factor auth");
        fullLogin(accountName, newPassword, null);
        log.info("Success!");
    }

    protected AuthResponse fullLogin(String accountName, String password, String deviceId) throws Exception {
        RestResponse response;
        response = login(accountName, password, deviceId);
        assertEquals(200, response.status);

        AuthResponse authResponse = fromJson(response.json, CloudOsAuthResponse.class);
        assertNotNull(authResponse.getSessionId());

        if (authResponse.isTwoFactor()) {
            final RestResponse secondFactorResponse = secondFactor(accountName, "0000000", deviceId);
            assertEquals(200, secondFactorResponse.status);
            authResponse = fromJson(secondFactorResponse.json, CloudOsAuthResponse.class);
            assertNotNull(authResponse.getSessionId());
        }
        return authResponse;
    }

    protected void expectFailedLogin(String accountName, String password) throws Exception {
        try {
            apiDocs.addNote("login with old password - should fail");
            fullLogin(accountName, password, null);
            fail("expected login with old password to fail");
        } catch (AssertionError ignored) {
            // expected
            log.info("OK, expected this: " + ignored);
        }
    }
}
