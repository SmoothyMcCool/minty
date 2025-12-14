package tom.api.task;

import java.util.Map;

public interface TaskSpec {

	// A description of what the shape of the input map should look like for this
	// task to make use of it. A description of each input port should be provided.
	String expects();

	// A description of shape of the map that is returned from runWorkflow(), for
	// each output port.
	String produces();

	// Number of output connectors this task has. These should all be described by
	// the text returned by produces().
	int numOutputs();

	// Number of inputs this task needs. Inputs are provided as arrays of Packets.
	// The expects() function should document how long each of the input arrays
	// should be.
	int numInputs();

	// Description of the configuration object for this task. Implementations should
	// ensure this TaskConfigSpec is populated with any default values for their
	// tasks.
	TaskConfigSpec taskConfiguration();

	// Description of the configuration object for this task, populated with the
	// given configuration.
	TaskConfigSpec taskConfiguration(Map<String, String> configuration);

	// Name of the task to run
	String taskName();

	// A one-word description of what kind of task this is. For GUI organization.
	// Suggested groups: Interface, Transformation, Flow Control, Output
	String group();
}
