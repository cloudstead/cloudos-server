package cloudos.model.support;

import lombok.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @ToString
public class AppInstallRequest {

    // If installing directly from app store
    @Getter @Setter private String publisher;

    @Getter @Setter private String name;

    @Getter @Setter private String version;
    public boolean hasVersion() { return !empty(version) && !version.equals("null") && !version.equals("latest"); }

    @Getter @Setter private boolean force;

    public AppInstallRequest (String name, String version, boolean force) {
        this(null, name, version, force);
    }

    public AppInstallRequest (String name, String version) {
        this(null, name, version, false);
    }

}
