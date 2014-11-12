package cloudos.service.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class TaskEvent {

    @Getter @Setter private String taskId;
    @Getter @Setter private String messageKey;
    @Getter @Setter private long ctime = System.currentTimeMillis();

    public TaskEvent(TaskBase task, String messageKey) {
        this.taskId = task.getTaskId().getUuid();
        this.messageKey = messageKey;
    }

}
