package tom.task;

import java.util.List;
import java.util.Map;

public interface AiTask {

	// Set a unique identifiable name for this task, to help with tracing through
	// logs.
	String taskName();

	// The results of this Task. This may be, but is not necessarily the same as the
	// output of doWork. This result represents all or a portion of the final output
	// of the overall workflow (for example, information retrieved from a webpage
	// during this Task).
	Map<String, Object> getResult();

	// Run in workflow mode. This task is expected to return information used in the
	// construction of tasks in the next workflow step.
	// Override this method, and do some work that generates results for the next
	// stage in the workflow to consume. One task is generated per item in the list.
	// If no items are generated, the workflow stops.
	List<Map<String, String>> runWorkflow();

	// Run in individual task mode. This task is not run as part of a workflow. As
	// part of execution, it may generate more tasks that will be executed after
	// this task completes. For example, it may be necessary to generate an output
	// task in order to get a result from this task.
	List<AiTask> runTask();

	// If this Task is part of a workflow, and not the initial Task, this is the
	// output of the previous Task for this Task to process.
	// For this to have any effect, this object should look like the config object
	// of any downstream tasks.
	void setInput(Map<String, String> input);

}
