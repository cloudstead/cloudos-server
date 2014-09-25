package cloudos.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class PasswordChangeRequest {

    @HasValue(message="error.changePassword.oldPassword.required")
    @Getter @Setter private String oldPassword;

    @HasValue(message="error.changePassword.newPassword.required")
    @Getter @Setter private String newPassword;

    @Getter @Setter private boolean sendInvite = false;

    public PasswordChangeRequest(String oldPassword, String newPassword) {
        this(oldPassword, newPassword, false);
    }
}
