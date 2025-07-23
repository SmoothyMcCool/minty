package tom.task.executor.service;

import java.io.IOException;
import java.util.List;

import tom.output.OutputTask;
import tom.task.AiTask;

public interface TaskExecutionService {

	String executeTask(AiTask task, OutputTask outputTask);

	List<String> getAvailableResults() throws IOException;

	String getResult(String taskName) throws IOException;

	void reportTaskComplete(TaskTracker tracker);

	boolean deleteResult(String resultName) throws IOException;

}
