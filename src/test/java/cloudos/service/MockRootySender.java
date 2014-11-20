package cloudos.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rooty.RootyConfiguration;
import rooty.RootyHandler;
import rooty.RootyMessage;
import rooty.RootySender;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MockRootySender extends RootySender {

    private RootyConfiguration rooty;

    public MockRootySender (RootyConfiguration rooty) { this.rooty = rooty; }

    @Getter private final List<RootyMessage> sent = new ArrayList<>();

    @Override public void write(RootyMessage message) {
        if (!message.hasUuid()) message.initUuid();

        // do not actually send the message, just handle it if we can...
        for (RootyHandler handler : rooty.getHandlers(message)) {
            try {
                handler.process(message);
                message.setSuccess(true);

            } catch (Exception e) {
                log.warn("Error handling message: " + e, e);
                message.setError(e.getMessage());

            } finally {
                handler.getStatusManager().update(getQueueName(), message.setFinished(true));
            }
        }

        sent.add(message);
    }

    public void flush() { sent.clear(); }

    public <T> T first(Class<T> messageClass) {
        for (RootyMessage m : sent) {
            if (messageClass.isAssignableFrom(m.getClass())) return (T) m;
        }
        return null;
    }

}
