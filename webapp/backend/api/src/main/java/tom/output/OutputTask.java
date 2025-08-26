package tom.output;

import java.io.IOException;

public interface OutputTask {

	// Generate the output from previous inputs.
	// Configuration contains the information required to configure this task (for
	// example, a rendering template).
	// Returns a Path to where the output is stored.
	String execute(ExecutionResult data) throws IOException;

	// Returns a content-type compliant string on how the output should be rendered
	// (such as text/html, text/json, etc).
	String getFormat();

}
