package cloudos.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.security.MD5Util;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity @Accessors(chain=true)
public class SslCertificate extends UniquelyNamedEntity {

    @Transient
    public String getCertName () { return getName(); }
    public void setCertName (String name) { setName(name); }

    @Getter @Setter private String description;
    @Getter @Setter private String commonName;
    @Getter @Setter private String pemSha;
    @Getter @Setter private String pemMd5;
    @Getter @Setter private String keySha;
    @Getter @Setter private String keyMd5;

    public SslCertificate setPem(String pem) {
        pemSha = ShaUtil.sha256_hex(pem);
        pemMd5 = MD5Util.md5hex(pem);
        return this;
    }

    public SslCertificate setKey(String key) {
        keySha = ShaUtil.sha256_hex(key);
        keyMd5 = MD5Util.md5hex(key);
        return this;
    }
}
