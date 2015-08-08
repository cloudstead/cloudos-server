package cloudos.service.task;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.task.TaskServiceBase;
import org.springframework.stereotype.Service;

@Service @Slf4j
public class TaskService extends TaskServiceBase<CloudOsTaskResult> {}
