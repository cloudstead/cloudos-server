package cloudos.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public class AccountNameList {

    @Getter private final List<String> accounts;

    public int getCount () { return accounts == null ? 0 : accounts.size(); }

}
