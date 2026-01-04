package tom.output.noop;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.ContentType;

import tom.api.task.ExecutionResult;
import tom.api.task.OutputTask;
import tom.api.task.OutputTaskSpec;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.api.task.annotation.Output;

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
				return ContentType.TEXT_PLAIN.getMimeType();
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
					public Map<String, Object> getValues() {
						return Map.of();
					}

				};
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return taskConfiguration();
			}

			@Override
			public String taskName() {
				return "No Output";
			}

		};
	}
}
