package cloudos.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Accessors(chain=true)
public class UnlockRequest {

    @Getter @Setter private SslCertificateRequest cert;
    @Getter @Setter private Map<String, String> settings;

}
