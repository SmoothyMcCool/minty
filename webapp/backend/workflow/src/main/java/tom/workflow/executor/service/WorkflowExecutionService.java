package tom.workflow.executor.service;

import java.io.IOException;
import java.util.List;

import tom.task.AiTask;

public interface WorkflowExecutionService {

	List<String> getAvailableResults() throws IOException;

	String getResult(String taskName) throws IOException;

	String executeTask(AiTask task);

	void reportTaskComplete(TaskTracker tracker);

}
