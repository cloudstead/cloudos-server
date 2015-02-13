package cloudos.model.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class CloudOsAppVersion {

    @Getter @Setter private String name;
    @Getter @Setter private String version;
    @Getter @Setter private String parent;
    @Getter @Setter private boolean installed;

}
