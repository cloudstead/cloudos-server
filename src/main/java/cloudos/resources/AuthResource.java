package cloudos.resources;

import cloudos.dao.AccountDAO;
import cloudos.model.Account;
import cloudos.server.CloudOsConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.AUTH_ENDPOINT)
@Service @Slf4j
public class AuthResource extends AuthResourceBase<Account> {

    @Autowired @Getter(value=AccessLevel.PROTECTED) protected AccountDAO accountDAO;
    @Autowired @Getter(value=AccessLevel.PROTECTED) protected TemplatedMailService templatedMailService;

    @Autowired private CloudOsConfiguration configuration;

    @Override protected String getResetPasswordUrl(String token) { return configuration.getResetPasswordUrl(token); }

    // todo: load these from a localized string table, and use cloudos's email domain (which might differ from hostname)
    @Override protected String getFromName(String templateName) { return "System Mailer for Cloudstead " + configuration.getHostname(); }
    @Override protected String getFromEmail(String templateName) { return "no-reply@" + configuration.getHostname(); }

}
