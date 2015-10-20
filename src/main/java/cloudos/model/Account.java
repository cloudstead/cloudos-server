package cloudos.model;

import cloudos.appstore.model.AppRuntimeDetails;
import cloudos.appstore.model.CloudOsAccount;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.system.Bytes;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.filters.CustomScrubbage;
import org.cobbzilla.wizard.filters.ScrubbableField;
import org.cobbzilla.wizard.ldap.FirstDnPartValueProducer;
import org.cobbzilla.wizard.model.ldap.*;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.wizard.model.ldap.LdapAttributeType.derivedAttr;
import static org.cobbzilla.wizard.model.ldap.LdapAttributeType.standardAttr;

@NoArgsConstructor @Accessors(chain=true)
//@JsonAutoDetect(fieldVisibility=NONE, getterVisibility=PUBLIC_ONLY)
public class Account extends LdapEntity implements CloudOsAccount, BasicAccount, CustomScrubbage {

    public Account (@JacksonInject(LDAP_CONTEXT) LdapContext context) { setLdapContext(context); }

    public Account (Account other) {
        setLdapContext(other.getLdapContext()); // now the copy will work :)
        copy(this, other);
        clean();
    }

    // jackson helper class
    public static final JavaType searchResultType = SearchResults.jsonType(Account.class);

    private static final ScrubbableField[] SCRUBBABLE = new ScrubbableField[]{
            new ScrubbableField(Account.class, "uuid", String.class),
            new ScrubbableField(Account.class, "authId", String.class),
            new ScrubbableField(Account.class, "password", String.class)
    };
    @Override @JsonIgnore public ScrubbableField[] fieldsToScrub() { return SCRUBBABLE; }

    @Override public void scrub(Object entity, ScrubbableField field) { remove(typeForJava(field.name).getLdapName()); }

    public static final String EMAIL_VERIFICATION_CODE = "emailVerificationCode";
    public static final String RESET_PASSWORD_TOKEN = "resetPasswordToken";

    @Override public String getIdField() { return ldap().getUser_username(); }
    @Override public String getParentDn() { return ldap().getUser_dn(); }

    private static final String[] LDAP_CLASSES = {"person","inetorgperson","organizationalperson","cloudosInetOrgPerson"};
    @Override public String[] getObjectClasses() { return LDAP_CLASSES; }

    private static final String[] REQUIRED_ATTRS = {"cn","uid"};
    @Override public String[] getRequiredAttributes() { return REQUIRED_ATTRS; }

    @Override protected List<LdapAttributeType> initLdapTypes() {
        final List<LdapAttributeType> types = super.initLdapTypes();
        types.add(standardAttr("uuid", ldap().getExternal_id()));
        types.add(standardAttr("email", ldap().getUser_email()));
        types.add(standardAttr("password", ldap().getUser_password()));
        types.add(standardAttr("firstName", ldap().getUser_firstname()));
        types.add(standardAttr("lastName", ldap().getUser_lastname()));
        types.add(standardAttr("mobilePhone", ldap().getUser_mobilePhone()));
        types.add(standardAttr("mobilePhoneCountryCode", ldap().getUser_mobilePhoneCountryCode()));
        types.add(standardAttr("locale", ldap().getUser_locale()));
        types.add(derivedAttr("name", ldap().getUser_username(), FirstDnPartValueProducer.instance));
        types.add(derivedAttr("fullName", ldap().getUser_username_rdn(), FullNameProducer.instance));
        types.add(derivedAttr("fullName", ldap().getUser_displayname(), FullNameProducer.instance));
        return types;
    }

    private String initToken(String tokenField) {
        final String token = RandomStringUtils.randomAlphanumeric(20);
        set(tokenField, token);
        set(tokenField + "Ctime", String.valueOf(System.currentTimeMillis()));
        return token;
    }

    public String initResetToken() { return initToken(RESET_PASSWORD_TOKEN); }

    public void clearResetPasswordToken() { remove(RESET_PASSWORD_TOKEN); }

    public void initEmailVerificationCode() { initToken(EMAIL_VERIFICATION_CODE); }
    public void clearEmailVerificationCode() { remove(EMAIL_VERIFICATION_CODE); }


    public boolean isEmailVerified () { return safeBoolean(getSingle("emailVerified"), false); }
    public void setEmailVerified(boolean verified) {
        if (verified) {
            set("emailVerified", ldapBoolean(true));
        } else {
            if (getAttrMap().contains("emailVerified")) {
                remove("emailVerified");
            }
        }
    }

    public static final String STATUS = "status";
    public enum Status { created, active, invited, suspended, admins, non_admins }

    public static final Comparator<Account> SORT_ACCOUNT_NAME = new Comparator<Account>() {
        @Override public int compare(Account a1, Account a2) {
            return a1 == null ? 1 : a2 == null ? -1 : String.valueOf(a1.getName()).compareTo(String.valueOf(a2.getName()));
        }
    };

    public String getEmail () { return getSingle(ldap().getUser_email()); }
    public Account setEmail (String value) { return (Account) set(ldap().getUser_email(), value); }

    public String getEmailVerificationCode () { return getSingle(EMAIL_VERIFICATION_CODE); }
    public Account setEmailVerificationCode (String value) { return (Account) set(EMAIL_VERIFICATION_CODE, value); }

    public Long getEmailVerificationCodeCtime () { return safeLong(getSingle(EMAIL_VERIFICATION_CODE+"Ctime")); }
    public Account setEmailVerificationCodeCtime (String value) { return (Account) set(EMAIL_VERIFICATION_CODE+"Ctime", value); }

    @Override public boolean isEmailVerificationCodeValid(long expiration) {
        Long ctime = getEmailVerificationCodeCtime();
        return (ctime != null && ctime + expiration < System.currentTimeMillis());
    }

    @JsonIgnore @Override public long getResetTokenAge() {
        Long ctime = safeLong(getSingle(RESET_PASSWORD_TOKEN + "Ctime"));
        if (ctime == null) return Long.MAX_VALUE;
        return System.currentTimeMillis() - ctime;
    }

    public String getResetToken() { return getSingle(RESET_PASSWORD_TOKEN); }
    @Override public void setResetToken(String token) { set(RESET_PASSWORD_TOKEN, token); }

    public String getFirstName () { return getSingle(ldap().getUser_firstname()); }
    public Account setFirstName (String value) { return (Account) set(ldap().getUser_firstname(), value); }

    public String getLastName () { return getSingle(ldap().getUser_lastname()); }
    public Account setLastName (String value) { return (Account) set(ldap().getUser_lastname(), value); }

    @Transient @JsonIgnore public String getFullName() { return getFirstName() + " " + getLastName(); }
    @Transient @JsonIgnore public String getLastNameFirstName() { return getLastName() + ", " + getFirstName(); }

    public String getMobilePhone () { return getSingle(ldap().getUser_mobilePhone()); }
    public Account setMobilePhone (String value) { return (Account) set(ldap().getUser_mobilePhone(), value); }

    public Integer getMobilePhoneCountryCode () { return safeInt(getSingle(ldap().getUser_mobilePhoneCountryCode())); }
    public Account setMobilePhoneCountryCode (int value) { return (Account) set(ldap().getUser_mobilePhoneCountryCode(), String.valueOf(value)); }

    @Transient @JsonIgnore public String getMobilePhoneCountryCodeString() { return String.valueOf(getMobilePhoneCountryCode()); }

    public boolean isAdmin () { return Boolean.valueOf(getSingle(ldap().getUser_admin())); }

    public Account setAdmin (boolean value) { return (Account) set(ldap().getUser_admin(), ldapBoolean(value)); }

    public boolean isSuspended () { return safeBoolean(getSingle(ldap().getUser_suspended()), false); }
    public Account setSuspended (boolean value) { return (Account) set(ldap().getUser_suspended(), ldapBoolean(value)); }

    public boolean isTwoFactor () { return safeBoolean(getSingle(ldap().getUser_twoFactor()), false); }
    public Account setTwoFactor (boolean value) { return (Account) set(ldap().getUser_twoFactor(), ldapBoolean(value)); }

    public String getAuthId() { return getSingle(ldap().getUser_twoFactorAuthId()); }
    public Account setAuthId(String value) { return (Account) set(ldap().getUser_twoFactorAuthId(), String.valueOf(value)); }

    public boolean hasAuthId() { return !empty(getAuthId()); }
    @Transient @JsonIgnore public Integer getAuthIdInt() { return safeInt(getSingle(ldap().getUser_twoFactorAuthId())); }
    public Account setAuthIdInt(int value) { return setAuthId(String.valueOf(value)); }

    public Long getLastLogin () { return safeLong(getSingle(ldap().getUser_lastLogin())); }
    public Account setLastLogin (long value) { return (Account) set(ldap().getUser_lastLogin(), String.valueOf(value)); }
    public void setLastLogin() { setLastLogin(System.currentTimeMillis()); }
    public boolean hasLastLogin () { return getLastLogin() != null; }

    public String getLocale () { return getSingle(ldap().getUser_locale()); }
    public Account setLocale (String value) { return (Account) set(ldap().getUser_locale(), value); }
    public boolean hasLocale() { return !empty(getLocale()); }

    public Long getStorageQuota() { return safeLong(getSingle(ldap().getUser_storageQuota())); }
    public Account setStorageQuota (long value) { return (Account) set(ldap().getUser_storageQuota(), String.valueOf(value)); }

    @Transient public String getStorageQuotaString () { return Bytes.format(getStorageQuota()); }
    public Account setStorageQuotaString (String value) { return setStorageQuota(Bytes.parse(value)); }

    private void applyDelta(LdapAttributeDelta delta) {
        switch (delta.getOperation()) {
            case add:
                append(delta.getAttribute().getName(), delta.getAttribute().getValue());
                break;
            case replace:
                set(delta.getAttribute().getName(), delta.getAttribute().getValue());
                break;
            case delete:
                remove(delta.getAttribute().getName());
                break;
            default:
                notSupported(delta.getOperation().name());
        }
    }

    // Validated at login (against LDAP) and placed into the session.
    // Not stored in plaintext, and scrubbed from any outbound JSON
    @Transient private String password;
    public String getPassword() { return !empty(password) ? password : getSingle(ldap().getUser_password()); }
    public Account setPassword (String p) {
        password = p;
        remove(ldap().getUser_password());
        if (!empty(p)) set(ldap().getUser_password(), p);
        return this;
    }
    public boolean hasPassword() { return !empty(getPassword()); }

    @Override public void attrFromLdif(String name, String value) {
        if (name.equalsIgnoreCase(ldap().getUser_password()) && !value.startsWith(": ")) {
            setPassword(value);
        } else {
            super.attrFromLdif(name, value);
        }
    }

    // filled out by SessionDAO when it returns lookups
    @Transient @Getter @Setter private List<AppRuntimeDetails> availableApps = new ArrayList<>();

    public static class FullNameProducer implements LdapDerivedValueProducer<Account> {
        public static FullNameProducer instance = new FullNameProducer();
        @Override public String deriveValue(Account account) { return account.getFullName(); }
        @Override public String[] deriveValues(Account entity) { return notSupported(); }
    }
}
