package cloudos.service;

import lombok.Getter;
import org.cobbzilla.mail.TemplatedMailSender;
import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.mail.sender.SmtpMailSender;
import cloudos.server.CloudOsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class TemplatedMailService {

    public static final String T_WELCOME = "welcome";

    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_ADMIN = "admin";
    public static final String PARAM_HOSTNAME = "hostname";
    public static final String PARAM_PASSWORD = "password";

    @Autowired protected CloudOsConfiguration configuration;

    @Getter(lazy=true) private final TemplatedMailSender mailSender = initMailSender();

    protected TemplatedMailSender initMailSender() {

        final SmtpMailConfig smtpMailConfig = configuration.getSmtpMailConfig();
        final File fileRoot = new File(configuration.getEmailTemplateRoot());

        return new TemplatedMailSender(new SmtpMailSender(smtpMailConfig), fileRoot);
    }

}
