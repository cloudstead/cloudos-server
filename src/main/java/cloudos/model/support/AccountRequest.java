package cloudos.model.support;

import cloudos.model.Account;
import com.fasterxml.jackson.annotation.JacksonInject;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ldap.LdapContext;

@NoArgsConstructor @Accessors(chain=true)
public class AccountRequest extends Account {

    public AccountRequest(@JacksonInject(LDAP_CONTEXT) LdapContext context) { super(context); }

}
