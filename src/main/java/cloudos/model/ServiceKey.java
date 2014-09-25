package cloudos.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

@Accessors(chain=true)
public class ServiceKey extends UniquelyNamedEntity<ServiceKey> {

    @Getter @Setter private String publicKey;

}
