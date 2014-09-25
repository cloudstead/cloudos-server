package cloudos.resources.setup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class SetupSettings {

    @Getter @Setter private String secret;
    @Getter @Setter private String email;
    @Getter @Setter private String passwordHash;

}
