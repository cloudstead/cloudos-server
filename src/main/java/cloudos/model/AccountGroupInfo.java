package cloudos.model;

import lombok.*;
import lombok.experimental.Accessors;
import org.cobbzilla.util.reflect.ReflectionUtil;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static cloudos.resources.MessageConstants.ERR_DESCRIPTION_LENGTH;
import static cloudos.resources.MessageConstants.ERR_STORAGE_QUOTA_INVALID;
import static cloudos.resources.MessageConstants.ERR_STORAGE_QUOTA_LENGTH;
import static org.cobbzilla.util.string.StringUtil.BYTES_PATTERN;

@Embeddable @EqualsAndHashCode(callSuper=false)
@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class AccountGroupInfo {

    public AccountGroupInfo (AccountGroupInfo other) { ReflectionUtil.copy(this, other); }

    @Pattern(regexp=BYTES_PATTERN, message=ERR_STORAGE_QUOTA_INVALID)
    @Column(length=10) @Size(max=10, message=ERR_STORAGE_QUOTA_LENGTH)
    @Getter @Setter private String storageQuota;
    public boolean hasStorageQuota() { return storageQuota != null; }

    @Column(length=200) @Size(max=200, message=ERR_DESCRIPTION_LENGTH)
    @Getter @Setter private String description;

}
