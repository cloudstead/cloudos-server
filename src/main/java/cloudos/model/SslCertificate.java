package cloudos.model;

import lombok.experimental.Accessors;

import javax.persistence.Entity;

@Entity @Accessors(chain=true)
public class SslCertificate extends SslCertificateBase {}
