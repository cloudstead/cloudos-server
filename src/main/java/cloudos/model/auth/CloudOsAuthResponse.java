package cloudos.model.auth;

import cloudos.model.Account;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CloudOsAuthResponse extends AuthResponse<Account> {

    public static final CloudOsAuthResponse TWO_FACTOR = new CloudOsAuthResponse(true);

    private CloudOsAuthResponse(boolean twoFactor) { setSessionId(TWO_FACTOR_SID); }

    public CloudOsAuthResponse(String sessionId, Account account) { super(sessionId, account); }

}
