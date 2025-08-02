package tom.output.json;

import java.io.IOException;
import java.nio.file.Path;

import tom.output.ExecutionResult;
import tom.output.OutputTask;
import tom.output.annotations.Output;
import tom.task.ServiceConsumer;
import tom.task.services.TaskServices;

@Output(name = "JSON Output", configClass = "tom.output.json.JsonOutputConfig")
public class JsonOutput implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;
	private JsonOutputConfig configuration;

	public JsonOutput(JsonOutputConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public Path execute(ExecutionResult data) throws IOException {
		return taskServices.getRenderService().renderJson(configuration.getOutputFilename(), data);
	}

}
