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

	// Run the task. This task is expected to return information used in the
	// construction of tasks in the next workflow step.
	// Override this method, and do some work that generates results for the next
	// stage in the workflow to consume. One task is generated per item in the list.
	// If no items are generated, no further workflow steps are generated.
	List<Map<String, String>> runTask();

	// If this Task is part of a workflow, and not the initial Task, this is the
	// output of the previous Task for this Task to process.
	// For this to have any effect, this object should look like the config object
	// of any downstream tasks.
	void setInput(Map<String, String> input);

	// A description of what the shape of the input map should look like for this
	// task to make use of it.
	String expects();

	// A description of shape of the map that is returned from runWorkflow()
	String produces();
}
