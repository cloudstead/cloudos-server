package cloudos.model.support;

import lombok.*;

@NoArgsConstructor @AllArgsConstructor @ToString
public class AppInstallRequest {

    @Getter @Setter private String name;
    @Getter @Setter private String version;
    @Getter @Setter private boolean force;

}
