package cloudos.service;

import cloudos.appstore.model.app.config.AppConfigValidationResolver;
import cloudos.dao.AccountDAO;
import cloudos.dao.AccountGroupDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service @Slf4j
public class CloudOsAppConfigValidationResolver implements AppConfigValidationResolver {

    @Autowired private AccountGroupDAO groupDAO;
    @Autowired private AccountDAO accountDAO;

    @Override public boolean isValidGroup(String name) { return groupDAO.findByName(name) != null; }

    @Override public boolean isValidAccount(String name) { return accountDAO.findByName(name) != null; }

}
