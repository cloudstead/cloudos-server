package cloudos.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.validation.constraints.Size;

@Accessors(chain=true)
public class ServiceKey extends UniquelyNamedEntity {

    @HasValue(message="err.publicKey.empty")
    @Column(length=1024, unique=true, nullable=false, updatable=false)
    @Size(max=1024)
    @Getter @Setter private String publicKey;

}
