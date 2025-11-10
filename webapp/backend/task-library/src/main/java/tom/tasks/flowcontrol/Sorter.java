package tom.tasks.flowcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;
import tom.tasks.noop.NullTaskConfig;

@RunnableTask
public class Sorter implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private List<Packet> input;
	private boolean allInputRecevied;
	private boolean failed;

	public Sorter() {
		input = new ArrayList<>();
		allInputRecevied = false;
		failed = false;
	}

	public Sorter(TaskConfigSpec config) {
		this();
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
		input.sort((left, right) -> left.getId().compareTo(right.getId()));

		for (OutputPort output : outputs) {
			for (Packet p : input) {
				logger.debug("Sorter: sending packet with Id: " + p.getId());
				output.write(p);
			}
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		logger.debug("Sorter: Received packet of ID " + dataPacket.getId());
		input.add(dataPacket);
		return false;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return allInputRecevied;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String expects() {
				return "Any packets. This task only runs once, and it will only run once it has received all input from the previous step.";
			}

			@Override
			public String produces() {
				return "Each input is sent unmodified to the output, sorted by ID.";
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
				return new NullTaskConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new NullTaskConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Sorter";
			}

			@Override
			public String group() {
				return "Flow Control";
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		allInputRecevied = true;
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
