package cloudos.dao;

import cloudos.model.EmailDomain;
import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import rooty.events.email.NewEmailDomainEvent;
import rooty.events.email.RemoveEmailDomainEvent;

@Repository @Slf4j
public class EmailDomainDAO extends AbstractCRUDDAO<EmailDomain> {

    @Autowired private CloudOsConfiguration configuration;

    public EmailDomain findByName(String domainName) { return findByUniqueField("name", domainName); }

    @Override
    public EmailDomain postCreate(EmailDomain emailDomain, Object context) {

        configuration.getRooty().getSender()
                .write(new NewEmailDomainEvent(emailDomain.getName()));

        return super.postCreate(emailDomain, context);
    }

    @Override
    public void delete(String uuid) {

        final EmailDomain emailDomain = findByUuid(uuid);
        configuration.getRooty().getSender()
                .write(new RemoveEmailDomainEvent(emailDomain.getName()));

        super.delete(uuid);
    }

}
