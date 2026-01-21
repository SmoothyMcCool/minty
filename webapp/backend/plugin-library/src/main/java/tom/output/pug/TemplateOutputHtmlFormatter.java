package tom.output.pug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.ContentType;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.task.ExecutionResult;
import tom.api.task.OutputTask;
import tom.api.task.OutputTaskSpec;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.annotation.Output;

@Output
public class TemplateOutputHtmlFormatter implements OutputTask, ServiceConsumer {

	private TemplateOutputHtmlFormatterConfig config;
	private PluginServices pluginServices;

	public TemplateOutputHtmlFormatter() {
	}

	public TemplateOutputHtmlFormatter(TemplateOutputHtmlFormatterConfig config) {
		this();
		this.config = config;
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public String execute(ExecutionResult data) throws IOException {
		// Amalgamate all task names that are tagged as part of the output into a single
		// "Task Results" item for the template to use.
		List<Packet> taskResults = new ArrayList<>();
		for (String taskName : config.getResultTasks()) {
			if (data.getResults().containsKey(taskName)) {
				taskResults.addAll(data.getResults().get(taskName));
			}
		}
		data.setResults(Map.of("Task Results", taskResults));

		String template = pluginServices.getRenderService().getOutputPugTemplate(config.getTemplate());
		return pluginServices.getRenderService().renderPug(template, data);
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
				return new TemplateOutputHtmlFormatterConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				try {
					return new TemplateOutputHtmlFormatterConfig(configuration);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Failed to read configuration.", e);
				}
			}

			@Override
			public String taskName() {
				return "Render Pug from Template";
			}

		};
	}

}
