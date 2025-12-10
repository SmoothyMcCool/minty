package tom.output.json;

import java.io.IOException;
import java.util.Map;

import tom.api.services.TaskServices;
import tom.model.ServiceConsumer;
import tom.task.ExecutionResult;
import tom.task.OutputTask;
import tom.task.OutputTaskSpec;
import tom.task.TaskConfigSpec;
import tom.task.annotation.Output;

@Output
public class JsonOutput implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;

	public JsonOutput() {
	}

	public JsonOutput(JsonOutputConfig configuration) {
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public String execute(ExecutionResult data) throws IOException {
		return taskServices.getRenderService().renderJson(data);
	}

	@Override
	public OutputTaskSpec getSpecification() {
		return new OutputTaskSpec() {

			@Override
			public String getFormat() {
				return "text/json";
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new JsonOutputConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new JsonOutputConfig(configuration);
			}

			@Override
			public String taskName() {
				return "JSON Output";
			}

		};
	}
}
