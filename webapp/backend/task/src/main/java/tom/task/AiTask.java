package tom.task;

import java.util.List;
import java.util.Map;

public interface AiTask {

    // Set a unique identifiable name for this task, to help with tracing through
    // logs.
    String taskName();

    Map<String, Object> getResult();

    String getResultTemplateFilename();

    // Override this method, and do some work that generates more tasks to do, or
    // null for no more work.
    List<AiTask> doWork();
}
