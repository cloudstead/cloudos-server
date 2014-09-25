package cloudos.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import cloudos.model.Account;
import org.cobbzilla.util.string.StringUtil;

@NoArgsConstructor @AllArgsConstructor
public class AuthResponse {

    @Getter @Setter private String sessionId;
    @Getter @Setter private Account account;

    public boolean hasSessionId() { return !StringUtil.empty(sessionId) && !isTwoFactor(); }

    public static final AuthResponse TWO_FACTOR = new AuthResponse("2-factor", null);

    @JsonIgnore public boolean isTwoFactor () { return TWO_FACTOR.getSessionId().equals(sessionId); }


}
