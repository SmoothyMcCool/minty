package tom.tasks.transform.htmlformatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class FormatHtml extends MintyTask implements ServiceConsumer {

	private List<? extends OutputPort> outputs;

	private FormatHtmlConfig config;
	private Packet input;
	private PluginServices pluginServices;
	private boolean failed;
	private String error;
	private Packet result;

	public FormatHtml() {
		input = null;
		outputs = null;
		failed = false;
		error = null;
		result = null;
	}

	public FormatHtml(FormatHtmlConfig config) {
		this();
		this.config = config;
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public Packet getResult() {
		return result;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void run() {
		try {

			String resultStr = pluginServices.getRenderService().renderPug(config.getTemplate(), input);

			result = new Packet();
			result.addText(resultStr);
			result.setId(input.getId());

			outputs.get(0).write(result);
		} catch (IOException e) {
			warn("Failed to generate pug template", e);
			failed = true;
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"FormatCustomHtml: Workflow misconfiguration detect. FormatCustomHtml should only ever have exactly one input!");
		}

		input = dataPacket;

		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			warn("Workflow misconfiguration detect. FormatCustomHtml should only ever have exactly one output!");
		}
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return input != null;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Render a Pug template you provide against the input packet, producing an HTML string "
						+ "stored in the output packet's text field. "
						+ "For a pre-built system template use Format Html (Template) instead.";
			}

			@Override
			public String expects() {
				return "Accepts: any packet. The full packet - id, text array, and data array - is passed "
						+ "to the Pug template as context. This task does not iterate over the data array "
						+ "automatically; use Pug's each directive inside your template to handle multiple records.";
			}

			@Override
			public String produces() {
				return "Emits: a packet with the rendered HTML string as the sole entry in the text list. "
						+ "The output packet ID is copied from the input. The data list is empty.";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new FormatHtmlConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new FormatHtmlConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Format Html";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public String html() {
		return "";
	}

}
