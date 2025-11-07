package tom.task;

import java.util.Map;

public interface OutputTaskSpec {

	// Returns a content-type compliant string on how the output should be rendered
	// (such as text/html, text/json, etc).
	String getFormat();

	// Description of the configuration object for this task.
	TaskConfigSpec taskConfiguration();

	// Description of the configuration object for this task, populated with the
	// given configuration.
	TaskConfigSpec taskConfiguration(Map<String, String> configuration);

	// Name of the task to run
	String taskName();
}
