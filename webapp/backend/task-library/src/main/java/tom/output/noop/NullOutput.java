package tom.output.noop;

import java.io.IOException;
import java.nio.file.Path;

import tom.output.ExecutionResult;
import tom.output.OutputTask;
import tom.output.annotations.Output;

@Output(name = "No-Op")
public class NullOutput implements OutputTask {

	public NullOutput() {
	}

	@Override
	public Path execute(ExecutionResult result) throws IOException {
		return null;
	}

}
