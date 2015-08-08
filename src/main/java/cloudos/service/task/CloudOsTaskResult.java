package cloudos.service.task;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.task.TaskResult;
import rooty.RootyMessage;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class CloudOsTaskResult extends TaskResult {

    @Getter @Setter
    private String rootyUuid;
    public boolean hasRootyUuid () { return !empty(rootyUuid); }

    @Getter @Setter private RootyMessage rootyStatus;

}
