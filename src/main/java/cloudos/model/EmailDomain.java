package cloudos.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity @NoArgsConstructor
@EqualsAndHashCode(of="name", callSuper=false)
public class EmailDomain extends IdentifiableBase {

    public EmailDomain (String name) { setName(name); }
    public EmailDomain (String name, boolean readOnly) { this(name); setReadOnly(readOnly); }

    @Column(unique=true, nullable=false, updatable=false)
    private String name;

    @Transient @Getter @Setter private boolean readOnly = false;

    // always lowercase
    public String getName () { return name.toLowerCase(); }
    public void setName (String name) { this.name = name.toLowerCase(); }

}
