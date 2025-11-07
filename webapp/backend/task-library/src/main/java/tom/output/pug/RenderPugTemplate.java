package tom.output.pug;

import java.io.IOException;
import java.util.Map;

import tom.api.services.TaskServices;
import tom.task.ExecutionResult;
import tom.task.OutputTask;
import tom.task.OutputTaskSpec;
import tom.task.ServiceConsumer;
import tom.task.TaskConfigSpec;
import tom.task.annotation.Output;

@Output
public class RenderPugTemplate implements OutputTask, ServiceConsumer {

	private TaskServices taskServices;
	private RenderPugTemplateConfig configuration;

	public RenderPugTemplate() {
		this.taskServices = null;
		this.configuration = null;
	}

	public RenderPugTemplate(RenderPugTemplateConfig configuration) {
		this();
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
	public OutputTaskSpec getSpecification() {
		return new OutputTaskSpec() {

			@Override
			public String getFormat() {
				return "text/html";
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new RenderPugTemplateConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new RenderPugTemplateConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Render Pug HTML Template";
			}

		};
	}
}
