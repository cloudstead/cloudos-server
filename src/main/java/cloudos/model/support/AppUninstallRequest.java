package cloudos.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class AppUninstallRequest {

    @Getter @Setter private String name;
    @Getter @Setter private AppUninstallMode mode;

    public AppUninstallRequest (AppUninstallMode mode) { setMode(mode); }

}
