package cloudos.service;

import cloudos.model.support.AccountRequest;
import cloudos.model.support.AuthenticationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandResult;

import java.util.HashMap;
import java.util.Map;

import static cloudos.model.support.AuthenticationException.Problem.NOT_FOUND;

@Slf4j
public class MockKerberosService extends KerberosService {

    private static final CommandResult SUCCESS = new CommandResult(0, "", "");
    @Getter private Map<String, String> passwordMap = new HashMap<>();

    public String getPassword (String user) { return passwordMap.get(user); }

    @Override
    public CommandResult createPrincipal(AccountRequest request) {
        passwordMap.put(request.getName(), request.getPassword());
        return SUCCESS;
    }

    @Override
    public void authenticate(String accountName, String password) throws AuthenticationException {
        String foundPassword = passwordMap.get(accountName);
        if (foundPassword == null || !foundPassword.equals(password)) throw new AuthenticationException(NOT_FOUND);
    }

    @Override
    public void changePassword(String accountName, String oldPassword, String newPassword) throws AuthenticationException {
        authenticate(accountName, oldPassword);
        passwordMap.put(accountName, newPassword);
    }

    @Override
    public void adminChangePassword(String accountName, String newPassword) {
        passwordMap.put(accountName, newPassword);
    }

    @Override public void deletePrincipal(String accountName) { passwordMap.remove(accountName); }

}
