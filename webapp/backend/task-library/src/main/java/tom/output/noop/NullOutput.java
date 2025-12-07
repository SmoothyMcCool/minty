package tom.output.noop;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import tom.task.ExecutionResult;
import tom.task.OutputTask;
import tom.task.OutputTaskSpec;
import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;
import tom.task.annotation.Output;

@Output
public class NullOutput implements OutputTask {

	public NullOutput() {
	}

	public NullOutput(TaskConfigSpec anything) {
	}

	@Override
	public String execute(ExecutionResult data) throws IOException {
		return null;
	}

	@Override
	public OutputTaskSpec getSpecification() {
		return new OutputTaskSpec() {

			@Override
			public String getFormat() {
				return "text/plain";
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new TaskConfigSpec() {

					@Override
					public Map<String, TaskConfigTypes> getConfig() {
						return Map.of();
					}

					@Override
					public List<String> getSystemConfigVariables() {
						return List.of();
					}

					@Override
					public List<String> getUserConfigVariables() {
						return List.of();
					}

					@Override
					public Map<String, String> getValues() {
						return Map.of();
					}

				};
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return taskConfiguration();
			}

			@Override
			public String taskName() {
				return "No Output";
			}

		};
	}
}
