package tom.tasks.flowcontrol.grouping;

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
public class Normalize implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet input;
	private boolean failed;

	public Normalize() {
		input = null;
		failed = false;
	}

	public Normalize(TaskConfigSpec config) {
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
		for (OutputPort output : outputs) {
			input.getDataList().forEach(item -> {
				Packet out = new Packet();
				out.setId(input.getId());
				out.setText(input.getText());
				out.addData(item);
				output.write(out);
			});

		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		logger.info("Joiner: input " + inputNum + " got " + dataPacket);
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
				return "Any data packet.";
			}

			@Override
			public String produces() {
				return "If the packet data contains a list of objects, it is sent out as a sequence of packets, each containing one element of the list.";
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
				return "Normalizer";
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
		return failed;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
