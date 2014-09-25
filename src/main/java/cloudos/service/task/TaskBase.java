package cloudos.service.task;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;

public abstract class TaskBase implements Callable<TaskResult> {

    @Getter @Setter protected TaskId taskId;
    @Getter @Setter protected TaskResult result = new TaskResult();

    public void init() {
        taskId = new TaskId();
        taskId.initUUID();
    }

    protected void description(String actionMessageKey, String target) {
        result.description(actionMessageKey, target);
    }

    protected void addEvent(String messageKey) {
        result.add(new TaskEvent(this, messageKey));
    }

    protected void error(String messageKey, Exception e) {
        result.error(new TaskEvent(this, messageKey), e);
    }

}
