package tom.task.taskregistry;

import java.util.List;
import java.util.Map;

import tom.api.UserId;
import tom.output.OutputTask;
import tom.task.MintyTask;
import tom.workflow.model.TaskDescription;
import tom.workflow.model.TaskRequest;

public interface TaskRegistryService {

	MintyTask newTask(UserId userId, TaskRequest request);

	OutputTask newOutputTask(UserId userId, TaskRequest request);

	List<TaskDescription> getTasks();

	Map<String, String> getConfigForTask(String taskName);

	List<TaskDescription> getOutputTaskTemplates();

	Map<String, String> getConfigForOutputTask(String outputName);

	Map<String, String> getSystemDefaults();

	Map<String, String> getUserDefaults();

}
