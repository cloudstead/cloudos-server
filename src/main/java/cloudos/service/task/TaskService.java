package cloudos.service.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service @Slf4j
public class TaskService {

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Map<String, TaskBase> taskMap = new ConcurrentHashMap<>();

    public TaskId execute(TaskBase task) throws Exception {
        task.init();
        executor.submit(task);
        taskMap.put(task.getTaskId().getUuid(), task);
        return task.getTaskId();
    }

    public TaskResult getResult(String uuid) {
        TaskBase task = taskMap.get(uuid);
        return task == null ? null : task.getResult();
    }
}
