package cloudos.service;

import cloudos.server.CloudOsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.*;

import java.util.concurrent.TimeUnit;

@Service @Slf4j
public class RootyService {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    @Autowired protected CloudOsConfiguration configuration;

    public RootySender getSender () { return configuration.getRooty().getSender(); }

    public RootyStatusManager getStatusManager () { return configuration.getRooty().getStatusManager(); }

    public <T extends RootyHandler> T getHandler(Class<T> clazz) { return configuration.getRooty().getHandler(clazz); }

    public RootyMessage request(RootyMessage message) { return request(message, TIMEOUT); }

    public RootyMessage request(RootyMessage message, long timeout) {
        return RootyMain.request(message, getSender(), getStatusManager(), timeout);
    }

}
