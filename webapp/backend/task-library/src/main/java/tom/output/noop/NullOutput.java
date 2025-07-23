package tom.output.noop;

import java.nio.file.Path;
import java.util.Map;

import tom.output.OutputTask;
import tom.output.annotations.Output;

@Output(name = "No-Op")
public class NullOutput implements OutputTask {

	public NullOutput() {
	}

	@Override
	public Path execute(Map<String, Object> data) {
		return null;
	}

}
