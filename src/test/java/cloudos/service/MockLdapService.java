package cloudos.service;

import cloudos.model.Account;
import cloudos.model.AccountGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.CaseInsensitiveStringKeyMap;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.ldap.LdapEntity;
import org.cobbzilla.wizard.model.ldap.LdapOperation;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@Slf4j
public class MockLdapService extends CloudOsLdapService {

    @Getter(lazy=true) private final Map<String, Account> accounts = new CaseInsensitiveStringKeyMap<>();
    @Getter(lazy=true) private final Map<String, AccountGroup> groups = new CaseInsensitiveStringKeyMap<>();
    @Getter(lazy=true) private final Map<String, LdapEntity> everything = new CaseInsensitiveStringKeyMap<>();

    public void reset() {
        getAccounts().clear();
        getGroups().clear();
        getEverything().clear();
    }

    @Getter(lazy=true) private final Map<String, String> passwordMap = initPasswordMap();
    private Map<String, String> initPasswordMap() {
        final Map<String, String> map = new CaseInsensitiveStringKeyMap<>();
        map.put(getConfiguration().getAdmin_dn(), getConfiguration().getPassword());
        return map;
    }

    @Override public CommandResult ldapadd(String ldif) {
        if (isAccount(ldif)) {
            addAccount(accountDAO.fromLdif(ldif));

        } else if (isGroup(ldif)) {
            AccountGroup group = groupDAO.fromLdif(ldif);
            getGroups().put(group.getDn(), group);
            getEverything().put(group.getDn(), group);

        } else {
            return notSupported("ldapadd: "+ldif);
        }
        return CommandResult.OK;
    }

    protected void addAccount(Account account) {
        if (account == null) die("addAccount: null");
        getAccounts().put(account.getDn(), account);
        getEverything().put(account.getDn(), account);
        getPasswordMap().put(account.getDn(), account.getPassword());
    }

    @Override public CommandResult ldapmodify(String ldif) {
        LdapEntity entity = null;
        LdapOperation operation = null;
        String fieldToDelete = null;
        for (String line : ldif.split("\n")) {

            if (line.startsWith("dn: ")) {
                if (isAccountDn(line)) {
                    entity = getAccounts().get(line.substring("dn: ".length()));
                } else if (isGroupDn(line)) {
                    entity = getGroups().get(line.substring("dn: ".length()));
                } else {
                    notSupported(line);
                }
                if (entity == null) die("modify: No such object: "+line);
                continue;
            }
            if (line.startsWith("changetype")) {
                if (!line.endsWith("modify")) die("modify: invalid LDIF: "+ldif);
                continue;
            }
            if (line.trim().equals("-")) {
                if (operation == LdapOperation.delete) {
                    if (entity != null) entity.remove(fieldToDelete);
                    operation = null;
                }
                continue;
            }

            if (operation == null) {
                operation = LdapOperation.valueOf(line.split(":")[0]);
                if (operation == LdapOperation.delete) {
                    fieldToDelete = line.split(":")[1];
                }

            } else if (entity != null) {
                final String[] args = line.split(":");
                switch (operation) {
                    case add: entity.append(args[0].trim(), args[1].trim()); break;
                    case replace: entity.set(args[0].trim(), args[1].trim()); break;
                    case delete:
//                        if (empty(args[1])) {
//                            entity.remove(args[0].trim());
//                        } else {
                            entity.remove(args[0].trim(), args[1].trim());
//                        }
                        break;
                    default: notSupported(operation.name());
                }
                operation = null;
            }
        }
        if (entity != null) {
            entity.clean();

            // update password if needed
            if (entity instanceof Account) {
                getPasswordMap().put(entity.getDn(), ((Account) entity).getPassword());
            }
        }
        return CommandResult.OK;
    }

    @Override public CommandResult ldapdelete(String dn) {
        getAccounts().remove(dn);
        getGroups().remove(dn);
        getEverything().remove(dn);
        return CommandResult.OK;
    }

    @Override public String ldapsearch(String userDn, String password, ResultPage page) {

        authenticate(userDn, password);

        final Map<String, String> bounds = page.getBounds();
        final String dn = bounds == null ? null : bounds.remove(BOUND_DN);
        final String base = bounds == null ? null : bounds.remove(BOUND_BASE);

        Collection<? extends LdapEntity> candidates;
        final String header;
        if (base == null) {
            candidates = getEverything().values();
            header = EVERYTHING_HEADER;

        } else if (isAccountDn(base)) {
            candidates = getAccounts().values();
            header = PEOPLE_HEADER;

        } else if (isGroupDn(base)) {
            candidates = getGroups().values();
            header = GROUPS_HEADER;

        } else {
            return notSupported(base);
        }

        final List<LdapEntity> results = new ArrayList<>();
        if (!empty(dn)) {
            if (!empty(bounds)) die("ldapsearch: if bound '"+BOUND_DN+"' is set, no other bounds may be set");
            final LdapEntity found = getEverything().get(dn);
            if (found != null) results.add(found);
            return formatResults(header, results);
        }

        if (!empty(candidates)) {
            for (LdapEntity entity : candidates) {
                if (matches(entity, page)) results.add(copy(entity));
            }
        }
        if (page.getHasSortField()) {
            final ResultPage.SortOrder sortOrder = page.getSortType();
            final String sort = page.getSortField();
            Collections.sort(results, LdapEntity.comparator(sort, sortOrder));
        }

        return formatResults(header, results);
    }

    protected String formatResults(String header, List<LdapEntity> results) {
        final StringBuilder b = new StringBuilder(header);
        for (LdapEntity entity : results) {
            b.append(entity.ldifCreate()).append("\n");
        }
        b.append(searchFooter(results.size()));

        return b.toString();
    }

    private boolean matches(LdapEntity entity, ResultPage page) {
        final String filter = page.getFilter();
        final String name = entity.getName();
        if (!empty(filter)) {
            // just filter on name for now
            if (!name.toLowerCase().contains(filter.toLowerCase())) return false;
        }

        final Map<String, String> bounds = page.getBounds();
        if (!empty(bounds)) {
            for (Map.Entry<String, String> bound : bounds.entrySet()) {
                if (bound.getKey().equals(BOUND_NAME)) {
                    if (!bound.getValue().equals("*") && !name.equalsIgnoreCase(bound.getValue())) return false;
                    continue;
                }
                if (entity instanceof Account) {
                    final Account account = (Account) entity;
                    final String ldapField = account.typeForJava(bound.getKey()).getLdapName();
                    if (ldapField.equals(Account.STATUS)) {
                        switch (Account.Status.valueOf(bound.getValue())) {
                            case active:
                                if (!account.hasLastLogin() || account.isSuspended()) return false;
                                break;
                            case invited:
                                if (account.hasLastLogin() || account.isSuspended()) return false;
                                break;
                            case suspended:
                                if (!account.isSuspended()) return false;
                                break;
                            case admins:
                                if (!account.isAdmin() || account.isSuspended()) return false;
                                break;
                            case non_admins:
                                if (account.isAdmin()) return false;
                                break;
                            default:
                                return die("matches: invalid bound=value (" + bound.getKey() + "=" + bound.getValue() + ")");
                        }

                    } else if (ldapField.equals("resetPasswordToken")) {
                        if (!bound.getValue().equals(account.getResetToken())) return false;

                    } else {
                        notSupported("matches: unsupported bound '"+ldapField+"'");
                    }
                } else {
                    final AccountGroup group = (AccountGroup) entity;
                    final String ldapField = group.typeForJava(bound.getKey()).getLdapName();
                    if (ldapField.equals("mirror")) {
                        if (!group.hasMirror(bound.getValue())) return false;
                    } else {
                        notSupported("matches: unsupported bound '" + ldapField + "'");
                    }
                }
            }
        }
        return true; // everything else matches
    }

    protected String searchFooter(int size) {
        return "# search result\nsearch: "+(size+1)+"\nresult: 0 Success\n\n# numResponses: "+(size+1)+"\n# numEntries "+size+"\n";
    }

    public String getPassword (String user) {
        Account account = getAccounts().get(user);
        if (account == null) account = getAccountByName(user);
        if (account == null) return getPasswordMap().get(user);
        return account.getPassword();
    }

    protected Account getAccountByName(String name) { return getAccounts().get(getConfiguration().userDN(name)); }

    public void authenticate(String accountName, String password) {
        final String foundPassword = getPassword(accountName);
        if (foundPassword == null || !foundPassword.equals(password)) die("authenticate failed");
    }

    @Override public void changePassword(String accountName, String oldPassword, String newPassword) {
        authenticate(accountName, oldPassword);
        getAccountByName(accountName).setPassword(newPassword);
    }

    @Override public void adminChangePassword(String accountName, String newPassword) {
        getAccountByName(accountName).setPassword(newPassword);
    }

    private static final String EVERYTHING_HEADER = "# extended LDIF\n" +
            "#\n" +
            "# LDAPv3\n" +
            "# base <dc=cloudstead,dc=io> with scope subtree\n" +
            "# filter: (objectclass=*)\n" +
            "# requesting: ALL\n" +
            "#\n" +
            "\n" +
            "# cloudstead.io\n" +
            "dn: dc=cloudstead,dc=io\n" +
            "objectClass: top\n" +
            "objectClass: domain\n" +
            "dc: cloudstead\n" +
            "\n" +
            "# Directory Administrators, cloudstead.io\n" +
            "dn: cn=Directory Administrators,dc=cloudstead,dc=io\n" +
            "objectClass: top\n" +
            "objectClass: groupofuniquenames\n" +
            "cn: Directory Administrators\n" +
            "uniqueMember: cn=Directory Manager\n" +
            "\n" +
            "# Groups, cloudstead.io\n" +
            "dn: ou=Groups,dc=cloudstead,dc=io\n" +
            "objectClass: top\n" +
            "objectClass: organizationalunit\n" +
            "ou: Groups\n\n";

    private static final String PEOPLE_HEADER = "# extended LDIF\n" +
            "#\n" +
            "# LDAPv3\n" +
            "# base <ou=People,dc=cloudstead,dc=io> with scope subtree\n" +
            "# filter: (objectclass=*)\n" +
            "# requesting: ALL\n" +
            "#\n" +
            "\n" +
            "# People, cloudstead.io\n" +
            "dn: ou=People,dc=cloudstead,dc=io\n" +
            "objectClass: top\n" +
            "objectClass: organizationalunit\n" +
            "ou: People\n\n";

    private static final String GROUPS_HEADER = "# extended LDIF\n" +
            "#\n" +
            "# LDAPv3\n" +
            "# base <ou=Groups,dc=cloudstead,dc=io> with scope subtree\n" +
            "# filter: (objectclass=*)\n" +
            "# requesting: ALL\n" +
            "#\n" +
            "\n" +
            "# Groups, cloudstead.io\n" +
            "dn: ou=Groups,dc=cloudstead,dc=io\n" +
            "objectClass: top\n" +
            "objectClass: organizationalunit\n" +
            "ou: Groups\n\n";
}
