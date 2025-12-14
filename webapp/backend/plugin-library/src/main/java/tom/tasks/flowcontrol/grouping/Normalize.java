package tom.tasks.flowcontrol.grouping;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
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
			int dataLength = input.getData().size();
			int textLength = input.getText().size();
			int maxLength = dataLength > textLength ? dataLength : textLength;

			for (int i = 0; i < maxLength; i++) {
				Packet out = new Packet();
				out.setId(input.getId());
				if (i < dataLength) {
					out.addData(input.getData().get(i));
				}
				if (i < textLength) {
					out.addText(input.getText().get(i));
				}
				output.write(out);
			}
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		logger.info("Normalize: input " + inputNum + " got " + dataPacket);
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
