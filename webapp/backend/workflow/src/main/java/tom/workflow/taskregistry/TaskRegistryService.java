package tom.workflow.taskregistry;

import java.util.Map;

import tom.task.AiTask;
import tom.workflow.controller.WorkflowRequest;

public interface TaskRegistryService {

    AiTask newTask(int userId, WorkflowRequest request);

    Map<String, Map<String, String>> getWorkflows();

    Map<String, String> getConfigFor(String workflowName);

    ClassLoader getClassLoader();

}
