package tom.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface OutputTask {

	// Generate the output from previous inputs.
	// Configuration contains the information required to configure this task (for
	// example, a rendering template).
	// Returns a Path to where the output is stored.
	Path execute(Map<String, Object> data) throws IOException;

}
