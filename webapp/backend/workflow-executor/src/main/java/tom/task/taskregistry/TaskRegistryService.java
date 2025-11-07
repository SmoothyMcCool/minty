package tom.task.taskregistry;

import java.util.List;
import java.util.Map;

import tom.api.UserId;
import tom.task.OutputTask;
import tom.task.TaskSpec;
import tom.task.MintyTask;
import tom.task.enumspec.EnumSpec;
import tom.workflow.executor.TaskRequest;
import tom.workflow.model.OutputTaskSpecDescription;
import tom.workflow.model.TaskSpecDescription;

public interface TaskRegistryService {

	MintyTask newTask(UserId userId, TaskRequest request);

	OutputTask newOutputTask(UserId userId, TaskRequest request);

	List<TaskSpecDescription> getTaskDescriptions();

	Map<String, String> getConfigForTask(String taskName);

	List<OutputTaskSpecDescription> getOutputTaskDescriptions();

	Map<String, String> getConfigForOutputTask(String outputName);

	Map<String, String> getSystemDefaults();

	Map<String, String> getUserDefaults();

	TaskSpec getSpecForTask(String taskName);

	List<EnumSpec> getEnumerations(UserId userId);

}
