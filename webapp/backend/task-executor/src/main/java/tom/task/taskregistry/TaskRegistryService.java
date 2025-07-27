package tom.task.taskregistry;

import java.util.List;
import java.util.Map;

import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.controller.TaskRequest;
import tom.task.model.TaskDescription;

public interface TaskRegistryService {

	AiTask newTask(int userId, TaskRequest request);

	OutputTask newOutputTask(int userId, TaskRequest request);

	List<TaskDescription> getTasks();

	Map<String, String> getConfigForTask(String taskName);

	List<TaskDescription> getOutputTaskTemplates();

	Map<String, String> getConfigForOutputTask(String outputName);

}
