package cloudos.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public class SslCertificateRequest {

    @Getter @Setter private String name;
    @Getter @Setter private String description;
    @Getter @Setter private String pem;
    @Getter @Setter private String key;

}
