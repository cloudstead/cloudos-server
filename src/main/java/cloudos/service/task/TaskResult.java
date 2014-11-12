package cloudos.service.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class TaskResult {

    @Getter @Setter private String actionMessageKey;
    @Getter @Setter private String target;

    @Getter @Setter private boolean success = false;
    @Getter @Setter @JsonIgnore private Exception exception;

    public String getError () { return exception == null ? null : exception.toString(); }
    public void setError (String error) { exception = new Exception(error); }

    @Getter private List<TaskEvent> events = new ArrayList<>();

    public void add(TaskEvent event) {
        this.events.add(event);
    }

    public void error(TaskEvent event, Exception e) {
        add(event);
        this.exception = e;
    }

    public void description (String actionMessageKey, String target) {
        setActionMessageKey(actionMessageKey);
        setTarget(target);
    }

    @JsonIgnore public boolean isComplete () { return success || getError() != null; }

}
