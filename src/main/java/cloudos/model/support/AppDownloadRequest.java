package cloudos.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public class AppDownloadRequest {

    @Getter @Setter private String token;
    @Getter @Setter private String url;
    @Getter @Setter private boolean autoInstall = true;
    @Getter @Setter private boolean overwrite = true;

    @JsonIgnore public String getDownloadUrl() { return url + "?token="+token; }

}
