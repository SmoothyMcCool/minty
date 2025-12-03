package tom.tasks.format.htmlformatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import tom.api.services.TaskServices;
import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.ServiceConsumer;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;

@RunnableTask
public class HtmlFormatter implements MintyTask, ServiceConsumer {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private HtmlFormatterConfig config;
	private Packet input;
	private TaskServices taskServices;
	private boolean failed;
	private String error;
	private Packet result;

	public HtmlFormatter() {
		input = null;
		outputs = null;
		failed = false;
		error = null;
		result = null;
	}

	public HtmlFormatter(HtmlFormatterConfig config) {
		this();
		this.config = config;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
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
		String resultStr;
		try {
			resultStr = taskServices.getRenderService().renderPug(config.getTemplate(), input);
			result = new Packet();
			result.addText(resultStr);
			result.setId(input.getId());
			outputs.get(0).write(result);
		} catch (IOException e) {
			logger.warn("HtmlFormatter: Failed to generate pug template", e);
			failed = true;
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"HtmlFormatter: Workflow misconfiguration detect. HtmlFormatter should only ever have exactly one input!");
		}

		input = dataPacket;

		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn(
					"HtmlFormatter: Workflow misconfiguration detect. HtmlFormatter should only ever have exactly one output!");
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
			public String expects() {
				return "This task produces HTML output based on the configuration provided. "
						+ "This task will pass the entirety of the data to the templating engine. It will not iterate over the array in data.";
			}

			@Override
			public String produces() {
				return "A rendered HTML document. The result is returned in \"Text\".";
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
				return new HtmlFormatterConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new HtmlFormatterConfig(configuration);
			}

			@Override
			public String taskName() {
				return "HTML Formatter";
			}

			@Override
			public String group() {
				return "Formatting";
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
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

	@Override
	public String html() {
		return "";
	}
}
