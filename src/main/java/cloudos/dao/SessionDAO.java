package cloudos.dao;

import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.model.Account;
import cloudos.server.CloudOsConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public class SessionDAO extends AbstractSessionDAO<Account> {

    @Autowired private CloudOsConfiguration configuration;
    @Autowired private AppDAO appDAO;

    @Override protected Class<Account> getEntityClass() { return Account.class; }

    @Override protected String getPassphrase() { return configuration.getCloudConfig().getDataKey(); }

    @Override
    protected String toJson(Account account) {
        try {
            return JsonUtil.FULL_MAPPER.writeValueAsString(account);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected Account fromJson(String json) {
        final Account account = super.fromJson(json);

        // todo: sort these by the user's preference. this is the order they will be displayed.
        final ArrayList<AppRuntimeDetails> availableApps = new ArrayList<>(appDAO.getAvailableAppDetails().values());

        account.setAvailableApps(availableApps);
        return account;
    }

}
