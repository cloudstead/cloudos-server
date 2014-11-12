package cloudos.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public class AppInstallUrlRequest {

    @Getter @Setter private String url;

}
