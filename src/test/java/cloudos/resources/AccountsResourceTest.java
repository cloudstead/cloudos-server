package cloudos.resources;

import cloudos.model.Account;
import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountRequest;
import cloudos.model.support.PasswordChangeRequest;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Before;
import org.junit.Test;

import static cloudos.resources.ApiConstants.ACCOUNTS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;

public class AccountsResourceTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Account Management";

    @Before public void resetTokens() { flushTokens(); }

    public AuthResponse assertAccount(AccountRequest request) throws Exception {
        return assertAccount(request, null);
    }

    public AuthResponse assertAccount(AccountRequest request, String deviceId) throws Exception {

        final String accountName = request.getAccountName();

        // be admin for this part
        pushToken(adminToken);
        apiDocs.appendNote("creating a regular account: " + accountName);
        RestResponse response = put(ACCOUNTS_ENDPOINT + "/" + accountName, toJson(request));
        assertEquals(200, response.status);
        final Account account = fromJson(response.json, Account.class);
        assertNotNull(account);
        assertEquals(request.getEmail(), account.getEmail());

        // grab password from email
        final MockTemplatedMailSender sender = getTemplatedMailSender();
        assertEquals(1, sender.messageCount());
        final String password = sender.first().getParameters().get(TemplatedMailService.PARAM_PASSWORD).toString();
        sender.reset();

        // ensure kerberos was called, and password is the same
        assertEquals(password, getKerberos().getPassword(account.getName()));

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
        apiDocs.appendNote("login account " + accountName);
        return login(loginRequest);
    }

    public RestResponse login(String accountName, String password, String deviceId) throws Exception {
        final LoginRequest loginRequest = new LoginRequest()
                .setName(accountName)
                .setPassword(password)
                .setDeviceId(deviceId)
                .setDeviceName(deviceId);
        apiDocs.appendNote("login account " + accountName + " with device "+deviceId);
        return login(loginRequest);
    }

    public RestResponse secondFactor(String accountName, String secondFactor, String deviceId) throws Exception {

        apiDocs.appendNote("account requires 2-factor auth. verify that a request to view profile should fail, since login has not been completed");
        assertEquals(404, doGet(ACCOUNTS_ENDPOINT + "/" + accountName).status);

        final LoginRequest loginRequest = new LoginRequest()
                .setName(accountName)
                .setSecondFactor(secondFactor)
                .setDeviceId(deviceId)
                .setDeviceName(deviceId);
        apiDocs.appendNote("send 2-factor verification token for account " + accountName);
        return login(loginRequest);
    }

    @Test
    public void testListAllUsers () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "list all accounts");
        pushToken(adminToken);
        apiDocs.addNote("fetch all accounts");
        final Account[] accounts = fromJson(doGet(ACCOUNTS_ENDPOINT).json, Account[].class);
        assertTrue(accounts.length > 0);
    }

    @Test
    public void testCreateAccountWithSystemPassword () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create account with system password");
        final String accountName = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(accountName);
        assertAccount(request);
    }

    @Test
    public void testCreateAccountWithAdminSuppliedPassword () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create account with admin-supplied password");
        final String accountName = randomAlphanumeric(10);
        final String password = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(accountName, password, false);
        assertAccount(request);
    }

    @Test
    public void testSuspendAccount () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "suspend a account, verify their session is invalidated and they can not login");
        final String accountName = randomAlphanumeric(10).toLowerCase();
        final String password = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(accountName, password, false);
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

    @Test
    public void testCreateAccountWith2FactorAuth () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create a account with 2-factor authentication and login");
        final String accountName = randomAlphanumeric(10);
        final String password = randomAlphanumeric(10);
        final String device1 = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(accountName, password, false);
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

    @Test
    public void testAdminResetPassword () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "as an admin, change another account's password");

        RestResponse response;

        apiDocs.addNote("add a regular account");
        final String accountName = randomAlphanumeric(10).toLowerCase();
        final String password = randomAlphanumeric(10);
        final AccountRequest request = newAccountRequest(accountName, password, false);
        final AuthResponse authResponse = assertAccount(request);

        apiDocs.addNote("add a second account");
        final String secondAccountName = randomAlphanumeric(10).toLowerCase();
        final AuthResponse secondAuth = assertAccount(newAccountRequest(secondAccountName));

        apiDocs.addNote("as second account, try to change first account's password using admin endpoint, this will fail");
        final String newPassword = randomAlphanumeric(10);
        final PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest("-", newPassword);
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
}
