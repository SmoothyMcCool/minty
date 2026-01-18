package tom.tasks.flowcontrol.branching;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Join implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet input;
	private JoinConfig config;

	public Join() {
		input = null;
		config = null;
	}

	public Join(JoinConfig config) {
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
		logger.debug("Join: received packet of Id " + dataPacket.getId() + ", on port " + inputNum);
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
			public String description() {
				return "Merge multiple data streams into one.";
			}

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
				return new JoinConfig(Map.of(JoinConfig.NumInputs, "2"));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new JoinConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Join";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
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
