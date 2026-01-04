package tom.output.json;

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
public class JsonOutput implements OutputTask, ServiceConsumer {

	private PluginServices pluginServices;

	public JsonOutput() {
	}

	public JsonOutput(JsonOutputConfig configuration) {
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public String execute(ExecutionResult data) throws IOException {
		return pluginServices.getRenderService().renderJson(data);
	}

	@Override
	public OutputTaskSpec getSpecification() {
		return new OutputTaskSpec() {

			@Override
			public String getFormat() {
				return ContentType.APPLICATION_JSON.getMimeType();
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new JsonOutputConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new JsonOutputConfig(configuration);
			}

			@Override
			public String taskName() {
				return "JSON Output";
			}

		};
	}
}
