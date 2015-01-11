package cloudos.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.Pattern;

@Accessors(chain=true)
public class RestoreRequest {

    @HasValue(message="err.setupKey.empty")
    @Getter @Setter private String setupKey;

    @HasValue(message="err.initialPassword.empty")
    @Getter @Setter private String initialPassword;

    @HasValue(message="err.restoreKey.empty")
    @Getter @Setter private String restoreKey;

    @Pattern(regexp="((2[\\d]{3})(-[\\d]{2}(-[\\d]{2}(-[\\d]{1,6})?)?)?)?", message="err.restoreDate.invalid")
    @Getter @Setter private String restoreDatestamp = "";

    @Email(message="err.email.invalid")
    @Getter @Setter private String notifyEmail;

}
