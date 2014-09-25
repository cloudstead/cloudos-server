package cloudos.dao;

import cloudos.model.SslCertificate;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;
import org.springframework.stereotype.Repository;

@Repository public class SslCertificateDAO extends UniquelyNamedEntityDAO<SslCertificate> {}
