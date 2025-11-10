package tom.tasks.flowcontrol;

import java.util.List;
import java.util.Map;

import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;

@RunnableTask
public class Joiner implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet input;
	private JoinerConfig config;

	public Joiner() {
		input = null;
		config = null;
	}

	public Joiner(JoinerConfig config) {
		this();
		this.config = config;
	}

	@Override
	public Packet getResult() {
		return null;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void run() {
		for (OutputPort output : outputs) {
			output.write(input);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		logger.debug("Joiner: received packet of Id " + dataPacket.getId() + ", on port " + inputNum);
		input = dataPacket;
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
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
				return "Sends all received input on all connected outputs.";
			}

			@Override
			public String produces() {
				return "Each input is sent unmodified to each connected output.";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return config != null ? config.getNumInputs() : 2;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new JoinerConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new JoinerConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Joiner";
			}

			@Override
			public String group() {
				return "Flow Control";
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		// Nothing to do.
	}

	@Override
	public boolean failed() {
		return false;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
