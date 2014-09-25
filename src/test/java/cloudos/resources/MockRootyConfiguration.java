package cloudos.resources;

import cloudos.service.MockRootySender;
import rooty.mock.MockRootyStatusManager;
import lombok.Getter;
import org.cobbzilla.util.mq.virtual.DevNullMqClient;
import rooty.RootyConfiguration;
import rooty.RootyHandler;
import rooty.RootySender;

import java.util.HashMap;
import java.util.Map;

public class MockRootyConfiguration extends RootyConfiguration {

    @Getter private final RootySender sender = new MockRootySender(this);

    // skip memcached for status updates, use mock
    @Getter private final MockRootyStatusManager statusManager = new MockRootyStatusManager();
    @Getter private final DevNullMqClient mqClient = new DevNullMqClient();

    @Getter private final Map<String, RootyHandler> handlerMap = new HashMap<>();

}
