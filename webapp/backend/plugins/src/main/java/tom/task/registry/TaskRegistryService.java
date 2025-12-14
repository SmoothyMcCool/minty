package tom.task.registry;

import java.util.List;
import java.util.Map;

import tom.api.UserId;
import tom.api.task.MintyTask;
import tom.api.task.OutputTask;
import tom.api.task.TaskSpec;
import tom.api.task.enumspec.EnumSpec;
import tom.task.model.OutputTaskSpecDescription;
import tom.task.model.TaskRequest;
import tom.task.model.TaskSpecDescription;

public interface TaskRegistryService {

	MintyTask newTask(UserId userId, TaskRequest request);

	OutputTask newOutputTask(UserId userId, TaskRequest request);

	List<TaskSpecDescription> getTaskDescriptions();

	Map<String, String> getConfigForTask(String taskName);

	List<OutputTaskSpecDescription> getOutputTaskDescriptions();

	Map<String, String> getConfigForOutputTask(String outputName);

	TaskSpec getSpecForTask(String taskName);

	List<EnumSpec> getEnumerations(UserId userId);

	Map<String, String> getTaskHelpFiles();

	Map<String, String> getOutputHelpFiles();

	void loadRunnableTask(Class<?> loadedClass) throws TaskLoadFailureException;

	void loadOutputTask(Class<?> loadedClass) throws TaskLoadFailureException;

	void loadEnumSpecCreator(Class<?> loadedClass);

	void addTaskHelp(String fileName, String content);

	void addOutputTaskHelp(String fileName, String content);

	boolean hasTask(String fileName);

	boolean hasOutputTask(String fileName);

}
