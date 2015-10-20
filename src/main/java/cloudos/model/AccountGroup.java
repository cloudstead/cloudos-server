package cloudos.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.ldap.FirstDnPartValueProducer;
import org.cobbzilla.wizard.model.ldap.LdapAttributeType;
import org.cobbzilla.wizard.model.ldap.LdapContext;
import org.cobbzilla.wizard.model.ldap.LdapEntity;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.wizard.model.ldap.LdapAttributeType.derivedAttr;
import static org.cobbzilla.wizard.model.ldap.LdapAttributeType.multipleAttr;
import static org.cobbzilla.wizard.model.ldap.LdapAttributeType.standardAttr;
import static org.cobbzilla.wizard.validation.ValidationMessages.translateMessage;

@NoArgsConstructor @Accessors(chain=true) @ToString
public class AccountGroup extends LdapEntity implements Comparable<AccountGroup> {

    // jackson helper class
    public static final JavaType searchResultType = SearchResults.jsonType(AccountGroup.class);

    @Override public int compareTo(AccountGroup other) { return getName().compareTo(other.getName()); }

    public static final String DEFAULT_GROUP_NAME = "cloudos-users";
    public static final String DEFAULT_GROUP_DESCRIPTION = translateMessage("{groups.default.description}");
    public AccountGroup defaultGroup() { return new AccountGroup(ldap(), DEFAULT_GROUP_NAME, DEFAULT_GROUP_DESCRIPTION); }

    public static final String ADMIN_GROUP_NAME = "cloudos-admins";
    public static final String ADMIN_GROUP_DESCRIPTION = translateMessage("{groups.admin.description}");
    public AccountGroup adminGroup() { return new AccountGroup(ldap(), ADMIN_GROUP_NAME, ADMIN_GROUP_DESCRIPTION); }

    @Override public String getIdField() { return ldap().getGroup_name(); }
    @Override public String getParentDn() { return ldap().getGroup_dn(); }

    private static final String[] LDAP_CLASSES = {"top","groupOfUniqueNames","cloudosGroupOfUniqueNames"};
    @Override public String[] getObjectClasses() { return LDAP_CLASSES; }

    private static final String[] REQUIRED_ATTRS = {"cn"};
    @Override public String[] getRequiredAttributes() { return REQUIRED_ATTRS; }

    @Override protected List<LdapAttributeType> initLdapTypes() {
        final List<LdapAttributeType> types = super.initLdapTypes();
        types.add(derivedAttr("name", ldap().getGroup_name(), FirstDnPartValueProducer.instance));
        types.add(standardAttr("uuid", ldap().getExternal_id()));
        types.add(multipleAttr("members", ldap().getGroup_usernames()));
        types.add(multipleAttr("mirror", "mirror"));
        return types;
    }

    public AccountGroup(@JacksonInject(LDAP_CONTEXT) LdapContext ldapContext) { setLdapContext(ldapContext); }

    public AccountGroup(AccountGroup other) {
        setLdapContext(other.getLdapContext());
        copy(this, other);
        clean();
    }

    public AccountGroup(LdapContext ldapContext, String name, String description) {
        setLdapContext(ldapContext);
        setName(name);
        setDescription(description);
    }

    @Transient public List<String> getMirrors () { return get("mirror"); }
    public AccountGroup setMirrors (List<String> values) { return (AccountGroup) appendAll("mirror", values); }
    public AccountGroup addMirror (String dn) { return (AccountGroup) append("mirror", dn); }
    public void removeMirror (String dn) { remove("mirror", dn); }

    @JsonIgnore public boolean hasMirror() { return !empty(getMirrors()); }
    @JsonIgnore public boolean hasMirror(String name) {
        if (!hasMirror()) return false;
        for (String m : getMirrors()) if (m.equalsIgnoreCase(name)) return true;
        return false;
    }

    @Transient public String getDescription () { return getSingle(ldap().getGroup_description()); }
    public AccountGroup setDescription (String value) { return (AccountGroup) set(ldap().getGroup_description(), value); }

    @Transient public String getStorageQuota () { return getSingle("storageQuota"); }
    public AccountGroup setStorageQuota (String value) { return (AccountGroup) set("storageQuota", value); }

    @Transient public List<String> getMembers () {
        final List<String> members = get(ldap().getGroup_usernames());
        return members == null ? new ArrayList<String>() : members;
    }
    public void addMember(String dn) {
        if (!getMembers().contains(dn)) append(ldap().getGroup_usernames(), dn);
    }
    public void removeMember(String dn) { remove(ldap().getGroup_usernames(), dn); }

    public AccountGroup addMembers(List<String> members) {
        for (String m : members) addMember(m);
        return this;
    }

    public AccountGroup setMembers(List<String> memberDNs) {
        for (String dn : getMembers()) {
            if (!memberDNs.contains(dn)) removeMember(dn);
        }
        for (String dn : memberDNs) addMember(dn);
        return this;
    }

    public boolean hasMembers() { return !empty(getMembers()); }

}
