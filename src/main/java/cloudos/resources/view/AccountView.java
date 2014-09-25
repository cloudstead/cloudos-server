package cloudos.resources.view;

import cloudos.model.Account;
import cloudos.model.AccountBase;
import org.cobbzilla.util.reflect.ReflectionUtil;

public class AccountView extends AccountBase {

    public AccountView(Account account) {
        ReflectionUtil.copy(this, account);
    }

}
