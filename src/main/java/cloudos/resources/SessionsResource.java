package cloudos.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import org.cobbzilla.wizard.resources.AbstractSessionsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.SESSIONS_ENDPOINT)
@Service
@Slf4j
public class SessionsResource extends AbstractSessionsResource<Account> {

    @Autowired @Getter private SessionDAO sessionDAO;

}
