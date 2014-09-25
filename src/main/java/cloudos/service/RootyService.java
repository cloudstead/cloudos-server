package cloudos.service;

import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.RootyHandler;
import rooty.RootyMessage;
import rooty.RootySender;
import rooty.RootyStatusManager;

import java.util.concurrent.TimeUnit;

@Service @Slf4j
public class RootyService {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    @Autowired protected CloudOsConfiguration configuration;

    public RootySender getSender () { return configuration.getRooty().getSender(); }

    public RootyStatusManager getStatusManager () { return configuration.getRooty().getStatusManager(); }

    public <T extends RootyHandler> T getHandler(Class<T> clazz) { return configuration.getRooty().getHandler(clazz); }

    public RootyMessage request(RootyMessage message) {
        getSender().write(message);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TIMEOUT) {
            final RootyMessage status = getStatusManager().getStatus(message.getUuid());
            if (status != null) return status;
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Error while sleeping: "+e, e);
            }
        }
        throw new IllegalStateException("request timeout: "+message);
    }
}
