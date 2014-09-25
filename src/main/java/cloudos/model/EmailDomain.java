package cloudos.model;

import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class EmailDomain extends IdentifiableBase {

    @Column(unique=true, nullable=false, updatable=false)
    private String name;

    // always lowercase
    public String getName () { return name.toLowerCase(); }
    public void setName (String name) { this.name = name.toLowerCase(); }

}
