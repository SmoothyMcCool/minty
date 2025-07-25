package tom.output.pug;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import tom.output.OutputTask;
import tom.output.annotations.Output;
import tom.task.ServiceConsumer;
import tom.task.services.TaskServices;

@Output(name = "Render Pug Template", configClass = "tom.output.pug.RenderPugTemplateConfig")
public class RenderPugTemplate implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;
	private RenderPugTemplateConfig configuration;

	public RenderPugTemplate(RenderPugTemplateConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public Path execute(Map<String, Object> data) throws IOException {
		return taskServices.getRenderService().renderPug(configuration.getTemplate(), configuration.getOutputFilename(),
				data);
	}

}
