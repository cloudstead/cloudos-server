package cloudos.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

@Accessors(chain=true)
public class SetupRequest extends AccountRequest {

    @HasValue(message="err.setupKey.empty")
    @Getter @Setter private String setupKey;

    @HasValue(message="err.initialPassword.empty")
    @Getter @Setter private String initialPassword;

    @HasValue(message="err.systemTimeZone.empty")
    @Getter @Setter private Integer systemTimeZone;
    public boolean hasSystemTimeZone () { return systemTimeZone != null; }

}
