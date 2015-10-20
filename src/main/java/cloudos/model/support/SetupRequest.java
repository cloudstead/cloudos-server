package cloudos.model.support;

import com.fasterxml.jackson.annotation.JacksonInject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ldap.LdapContext;
import org.cobbzilla.wizard.validation.HasValue;

@NoArgsConstructor @Accessors(chain=true)
public class SetupRequest extends AccountRequest {

    public SetupRequest(@JacksonInject(LDAP_CONTEXT) LdapContext ldapContext) { super(ldapContext); }

    @HasValue(message="err.setupKey.empty")
    @Getter @Setter private String setupKey;

    @HasValue(message="err.initialPassword.empty")
    @Getter @Setter private String initialPassword;

    @HasValue(message="err.systemTimeZone.empty")
    @Getter @Setter private Integer systemTimeZone;

    public boolean hasSystemTimeZone () { return systemTimeZone != null; }

}
