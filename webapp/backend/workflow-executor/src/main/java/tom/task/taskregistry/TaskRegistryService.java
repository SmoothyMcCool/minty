package tom.task.taskregistry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.output.OutputTask;
import tom.task.AiTask;
import tom.workflow.model.TaskDescription;
import tom.workflow.model.TaskRequest;

public interface TaskRegistryService {

	AiTask newTask(UUID userId, TaskRequest request);

	OutputTask newOutputTask(UUID userId, TaskRequest request);

	List<TaskDescription> getTasks();

	Map<String, String> getConfigForTask(String taskName);

	List<TaskDescription> getOutputTaskTemplates();

	Map<String, String> getConfigForOutputTask(String outputName);

}
