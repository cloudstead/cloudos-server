package cloudos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.validator.constraints.Email;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static cloudos.resources.MessageConstants.ERR_STORAGE_QUOTA_INVALID;
import static cloudos.resources.MessageConstants.ERR_STORAGE_QUOTA_LENGTH;
import static org.cobbzilla.util.string.StringUtil.BYTES_PATTERN;

@MappedSuperclass @Accessors(chain=true)
public class AccountBase extends UniquelyNamedEntity {

    public static final String ERR_AUTHID_LENGTH = "{err.authid.length}";
    public static final String ERR_EMAIL_INVALID = "{err.email.invalid}";
    public static final String ERR_EMAIL_EMPTY = "{err.email.empty}";
    public static final String ERR_EMAIL_LENGTH = "{err.email.length}";
    public static final String ERR_LAST_NAME_EMPTY = "{err.lastName.empty}";
    public static final String ERR_LAST_NAME_LENGTH = "{err.lastName.length}";
    public static final String ERR_FIRST_NAME_EMPTY = "{err.firstName.empty}";
    public static final String ERR_FIRST_NAME_LENGTH = "{err.firstName.length}";
    public static final String ERR_MOBILEPHONE_LENGTH = "{err.mobilePhone.length}";
    public static final String ERR_MOBILEPHONE_EMPTY = "{err.mobilePhone.empty}";
    public static final String ERR_MOBILEPHONE_CC_EMPTY = "{err.mobilePhoneCountryCode.empty}";
    public static final String ERR_PRIMARY_GROUP_LENGTH = "{err.primaryGroup.length}";
    public static final int EMAIL_MAXLEN = 1024;
    public static final int LASTNAME_MAXLEN = 25;
    public static final int FIRSTNAME_MAXLEN = 25;
    public static final int MOBILEPHONE_MAXLEN = 30;
    public static final int PRIMARY_GROUP_MAXLEN = 100;

    @Size(max=30, message=ERR_AUTHID_LENGTH)
    @Getter @Setter private String authId = null;

    public boolean hasAuthId() { return !StringUtil.empty(authId); }

    @JsonIgnore @Transient public Integer getAuthIdInt() { return authId == null ? null : Integer.valueOf(authId); }
    public AccountBase setAuthIdInt(int authId) { setAuthId(String.valueOf(authId)); return this; }

    @Transient
    public String getAccountName () { return getName(); }
    public AccountBase setAccountName (String name) { setName(name); return this; }

    @HasValue(message=ERR_LAST_NAME_EMPTY)
    @Size(max=LASTNAME_MAXLEN, message=ERR_LAST_NAME_LENGTH)
    @Column(nullable=false, length=LASTNAME_MAXLEN)
    @Getter @Setter private String lastName;

    @HasValue(message=ERR_FIRST_NAME_EMPTY)
    @Size(max=FIRSTNAME_MAXLEN, message=ERR_FIRST_NAME_LENGTH)
    @Column(nullable=false, length=FIRSTNAME_MAXLEN)
    @Getter @Setter private String firstName;

    @Pattern(regexp=BYTES_PATTERN, message=ERR_STORAGE_QUOTA_INVALID)
    @Column(length=10) @Size(max=10, message=ERR_STORAGE_QUOTA_LENGTH)
    @Getter @Setter private String storageQuota;

    @Size(max=PRIMARY_GROUP_MAXLEN, message=ERR_PRIMARY_GROUP_LENGTH)
    @Getter @Setter private String primaryGroup;

    @Getter @Setter private boolean admin = false;
    @Getter @Setter private boolean suspended = false;
    @Getter @Setter private boolean twoFactor = false;
    @Getter @Setter private Long lastLogin = null;
    public AccountBase setLastLogin () { lastLogin = System.currentTimeMillis(); return this; }

    @Email(message=ERR_EMAIL_INVALID)
    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Column(unique=true, nullable=false, length=EMAIL_MAXLEN)
    @Getter @Setter private String recoveryEmail;

    @Size(max=MOBILEPHONE_MAXLEN, message=ERR_MOBILEPHONE_LENGTH)
    @HasValue(message=ERR_MOBILEPHONE_EMPTY)
    @Getter @Setter private String mobilePhone;

    @HasValue(message= ERR_MOBILEPHONE_CC_EMPTY)
    @Getter @Setter private Integer mobilePhoneCountryCode;

    @JsonIgnore @Transient public String getMobilePhoneCountryCodeString() { return mobilePhoneCountryCode == null ? null : mobilePhoneCountryCode.toString(); }

}
