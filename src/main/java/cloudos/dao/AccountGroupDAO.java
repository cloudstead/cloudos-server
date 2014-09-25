package cloudos.dao;

import cloudos.model.AccountGroup;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.stereotype.Repository;

@Repository @Slf4j
public class AccountGroupDAO extends AbstractCRUDDAO<AccountGroup> {

    public AccountGroup findByName(String name) { return findByUniqueField("name", name); }

}
