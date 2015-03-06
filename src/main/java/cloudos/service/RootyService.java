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

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.system.Sleep.sleep;

@Service @Slf4j
public class RootyService {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    @Autowired protected CloudOsConfiguration configuration;

    public RootySender getSender () { return configuration.getRooty().getSender(); }

    public RootyStatusManager getStatusManager () { return configuration.getRooty().getStatusManager(); }

    public <T extends RootyHandler> T getHandler(Class<T> clazz) { return configuration.getRooty().getHandler(clazz); }

    public RootyMessage request(RootyMessage message) {
        return request(message, TIMEOUT);
    }

    public RootyMessage request(RootyMessage message, long timeout) {
        getSender().write(message);
        sleep(250, "waiting for rooty to complete");
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            final RootyMessage status = getStatusManager().getStatus(message.getUuid());
            if (status != null && status.isFinished()) return status;
            sleep(250, "waiting for rooty to complete");
        }
        return die("request timeout: " + message);
    }

}
