package tom.task.taskregistry;

import java.util.Map;

import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.controller.TaskRequest;

public interface TaskRegistryService {

	AiTask newTask(int userId, TaskRequest request);

	OutputTask newOutputTask(int userId, TaskRequest request);

	Map<String, Map<String, String>> getTasks();

	Map<String, String> getConfigForTask(String taskName);

	Map<String, Map<String, String>> getOutputTaskTemplates();

	Map<String, String> getConfigForOutputTask(String outputName);

	Map<String, Map<String, String>> listTaskConfigurations();

	Map<String, Map<String, String>> listOutputTaskConfigurations();
}
