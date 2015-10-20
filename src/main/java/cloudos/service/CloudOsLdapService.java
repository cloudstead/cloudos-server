package cloudos.service;

import cloudos.dao.AccountDAO;
import cloudos.dao.AccountGroupDAO;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractLdapDAO;
import org.cobbzilla.wizard.ldap.LdapServiceBase;
import org.cobbzilla.wizard.server.config.LdapConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;


@Service @Slf4j
public class CloudOsLdapService extends LdapServiceBase {

    @Autowired protected CloudOsConfiguration config;
    @Autowired protected AccountDAO accountDAO;
    @Autowired protected AccountGroupDAO groupDAO;

    @Override public LdapConfiguration getConfiguration() { return config.getLdap(); }
    public LdapConfiguration config() { return getConfiguration(); }

    protected boolean isAccount(String ldif) {
        return ldif.startsWith("dn: " + accountDAO.getTemplateObject().getIdField() + "=");
    }

    protected boolean isGroup(String ldif) {
        return ldif.startsWith("dn: " + groupDAO.getTemplateObject().getIdField() + "=");
    }

    protected boolean isAccountDn (String dn) { return dn.endsWith(getConfiguration().getUser_dn()); }
    protected boolean isGroupDn (String dn) { return dn.endsWith(getConfiguration().getGroup_dn()); }

    @Override protected String ldapFilter(String base, String filter, Map<String, String> bounds) {
        if (empty(base)) die("ldapFilter: no base specified");
        if (isAccountDn(base)) {
            return accountDAO.formatSearchFilter(filter, bounds);

        } else if (isGroupDn(base)) {
            return groupDAO.formatSearchFilter(filter, bounds);

        } else {
            die("ldapFilter: unsupported base: "+base);
        }
        return null;
    }

    @Override protected String ldapField(String base, String javaName) {
        if (empty(base)) die("ldapFilter: no base specified");
        final AbstractLdapDAO dao;
        if (isAccountDn(base)) {
            dao = accountDAO;
        } else if (isGroupDn(base)) {
            dao = groupDAO;
        } else {
            return die("ldapField: unsupported base: "+base);
        }
        return dao.getTemplateObject().typeForJava(javaName).getLdapName();
    }
}