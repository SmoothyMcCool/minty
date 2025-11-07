package tom.tasks.formatter.htmlformatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.services.TaskServices;
import tom.task.TaskSpec;
import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.ServiceConsumer;
import tom.task.TaskConfigSpec;
import tom.task.annotation.RunnableTask;

@RunnableTask
public class HtmlFormatter implements MintyTask, ServiceConsumer {

	private static final Logger logger = LogManager.getLogger(HtmlFormatter.class);

	private List<? extends OutputPort> outputs;

	private HtmlFormatterConfig config;
	private Packet input;
	private TaskServices taskServices;
	private boolean allInputReceived;
	private boolean failed;
	private String error;
	private Packet result;

	public HtmlFormatter() {
		input = new Packet();
		outputs = null;
		allInputReceived = false;
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
			result.setText(resultStr);
			result.setId(input.getId());
			outputs.get(0).write(result);
		} catch (IOException e) {
			logger.warn("Failed to generate pug template", e);
			failed = true;
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"Workflow misconfiguration detect. HtmlFormatter should only ever have exactly one input!");
		}

		input = dataPacket;

		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn("Workflow misconfiguration detect. HtmlFormatter should only ever have exactly one output!");
		}
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return allInputReceived;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String expects() {
				return "This task produces text output based on the configuration provided. "
						+ "It amalgamates all received input into a single record, and passes that to a pug template for rendering.";
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
		allInputReceived = true;
	}

	@Override
	public boolean failed() {
		return failed;
	}

}
