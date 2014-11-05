package cloudos.model;

import cloudos.appstore.model.app.AppManifest;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.concurrent.atomic.AtomicReference;

@Entity
public class InstalledApp extends UniquelyNamedEntity {

    // uuid references Account
    @Getter @Setter private String account;

    @Getter @Setter private boolean active;

    @Getter @Setter private String path;

    @Getter @Setter private String hostname;

    // set upon first install
    @Getter @Setter private int port;
    @Getter @Setter private int adminPort; // always generated, not always used

    @Size(max=4096, message="err.installedApp.manifestJson.tooLong")
    @Getter private String manifestJson;
    public void setManifestJson (String manifestJson) {
        this.manifestJson = manifestJson;
        this.manifest.set(null);
    }

    @Transient
    private final AtomicReference<AppManifest> manifest = new AtomicReference<>(null);

    @Transient
    public AppManifest getManifest () {
        try {
            if (manifest.get() == null) {
                manifest.set(JsonUtil.fromJson(manifestJson, AppManifest.class));
            }
            return manifest.get();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid manifestJson: "+manifestJson+": "+e, e);
        }
    }

    public void setManifest (AppManifest manifest) {
        try {
            manifestJson = JsonUtil.toJson(manifest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid manifest: "+manifest+": "+e, e);
        }
        this.manifest.set(manifest);
        name = manifest.getName();
        path = manifest.getPath();
        hostname = manifest.getHostname();
    }

    public String getLocalBaseUri() {
        final AppManifest m = getManifest();
        if (!m.hasWeb()) return null;
        return "http://127.0.0.1:"+getPort();
    }
}
