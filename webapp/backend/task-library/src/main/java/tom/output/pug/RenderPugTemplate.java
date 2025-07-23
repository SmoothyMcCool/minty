package tom.output.pug;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import tom.output.OutputTask;
import tom.output.annotations.Output;
import tom.task.ServiceConsumer;
import tom.task.services.TaskServices;

@Output(name = "Render Pug Template", configClass = "tom.output.pug.RenderPugTemplateConfig")
public class RenderPugTemplate implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;
	private RenderPugTemplateConfig configuration;
	UUID uuid;

	public RenderPugTemplate(RenderPugTemplateConfig configuration) {
		this.configuration = configuration;
		uuid = UUID.randomUUID();
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public Path execute(Map<String, Object> data) throws IOException {
		return taskServices.getPugRenderService().render(configuration.getTemplate(), configuration.getOutputFilename(),
				data);
	}

}
