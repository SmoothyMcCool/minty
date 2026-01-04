package tom.output.pug;

import java.io.IOException;
import java.util.Map;

import org.apache.hc.core5.http.ContentType;

import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.task.ExecutionResult;
import tom.api.task.OutputTask;
import tom.api.task.OutputTaskSpec;
import tom.api.task.TaskConfigSpec;
import tom.api.task.annotation.Output;

@Output
public class RenderPugTemplate implements OutputTask, ServiceConsumer {

	private PluginServices pluginServices;
	private RenderPugTemplateConfig configuration;

	public RenderPugTemplate() {
		this.pluginServices = null;
		this.configuration = null;
	}

	public RenderPugTemplate(RenderPugTemplateConfig configuration) {
		this();
		this.configuration = configuration;
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public String execute(ExecutionResult data) throws IOException {
		return pluginServices.getRenderService().renderPug(configuration.getTemplate(), data);
	}

	@Override
	public OutputTaskSpec getSpecification() {
		return new OutputTaskSpec() {

			@Override
			public String getFormat() {
				return ContentType.TEXT_HTML.getMimeType();
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new RenderPugTemplateConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new RenderPugTemplateConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Render Pug Template";
			}

		};
	}
}
