package tom.output.json;

import java.io.IOException;

import tom.api.services.TaskServices;
import tom.output.ExecutionResult;
import tom.output.OutputTask;
import tom.output.annotations.Output;
import tom.task.ServiceConsumer;

@Output(name = "JSON Output", configClass = "tom.output.json.JsonOutputConfig")
public class JsonOutput implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;

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
	public String getFormat() {
		return "text/json";
	}
}
