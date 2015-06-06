package cloudos.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true)
public class AppDownloadRequest {

    @Getter @Setter private String token;
    @Getter @Setter private String url;
    @Getter @Setter private String sha;
    public boolean hasSha () { return !empty(sha); }

    @Getter @Setter private boolean autoInstall = true;
    @Getter @Setter private boolean overwrite = true;

    @JsonIgnore public String getDownloadUrl() { return url + "?token="+token; }

}
