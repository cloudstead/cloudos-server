package cloudos.service;

import cloudos.service.task.TaskBase;
import cloudos.service.task.TaskResult;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rooty.RootyMessage;
import rooty.toots.restore.RestoreMessage;

import java.util.concurrent.TimeUnit;

/**
 * Restores a cloudstead from a previous backup
 */
@Accessors(chain=true) @Slf4j
public class RestoreTask extends TaskBase {

    private static final long RESTORE_TIMEOUT = TimeUnit.MINUTES.toMillis(90);

    @Getter @Setter private RootyService rootyService;
    @Getter @Setter private String restoreKey;
    @Getter @Setter private String restoreDatestamp;
    @Getter @Setter private String notifyEmail;

    @Override public TaskResult call() throws Exception {

        description("{restore.starting}", "-restoring-cloudstead-");

        final RestoreMessage restoreMessage = new RestoreMessage()
                .setRestoreKey(restoreKey)
                .setRestoreDatestamp(restoreDatestamp)
                .setNotifyEmail(notifyEmail);

        addEvent("{restore.restoring}");
        final RootyMessage status;
        try {
            result.setRootyUuid(restoreMessage.initUuid());
            status = rootyService.request(restoreMessage, RESTORE_TIMEOUT);

            // if successful, this should never return because the restore should restart cloudos

        } catch (Exception e) {
            error("{restore.error.restoring}", e);
            return null;
        }

        error("{restore.error.unknown}", "the restore did not complete successfully: "+status.getResults());
        return null;
    }

}
