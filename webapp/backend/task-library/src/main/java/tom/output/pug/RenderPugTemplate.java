package tom.output.pug;

import java.io.IOException;

import tom.api.services.TaskServices;
import tom.output.ExecutionResult;
import tom.output.OutputTask;
import tom.output.annotations.Output;
import tom.task.ServiceConsumer;

@Output(name = "Render Pug Template", configClass = "tom.output.pug.RenderPugTemplateConfig")
public class RenderPugTemplate implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;
	private final RenderPugTemplateConfig configuration;

	public RenderPugTemplate(RenderPugTemplateConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public String execute(ExecutionResult data) throws IOException {
		return taskServices.getRenderService().renderPug(configuration.getTemplate(), data);
	}

	@Override
	public String getFormat() {
		return "text/html";
	}
}
