package cloudos.model.support;

import cloudos.model.AccountBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

@Accessors(chain=true)
public class AccountRequest extends AccountBase {

    // optional, if no password set, then one will be generated and user will be instructed to login and change it
    @Getter @Setter private String password;
    @JsonIgnore public boolean hasPassword() { return !StringUtil.empty(password); }

}
