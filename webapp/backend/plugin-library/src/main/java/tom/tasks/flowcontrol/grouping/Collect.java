package tom.tasks.flowcontrol.grouping;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.Grouping;
import tom.tasks.TaskGroup;

@RunnableTask
public class Collect extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet result;
	private Grouping grouping;
	private String groupingId;
	private boolean readyToRun;

	public Collect() {
		result = new Packet();
		readyToRun = false;
	}

	public Collect(CollectConfig config) {
		this();
		this.grouping = config.getGrouping();
	}

	@Override
	public Packet getResult() {
		return result;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void run() {
		for (OutputPort output : outputs) {
			output.write(result);
		}
	}

	@Override
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		if (grouping == Grouping.ById) {
			if (groupingId == null) {
				return true;
			}
			if (!groupingId.equals(dataPacket.getId())) {
				readyToRun = true;
			}
			return groupingId.equals(dataPacket.getId());
		}

		return true;
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (groupingId == null) {
			groupingId = dataPacket.getId();
			result.setId(dataPacket.getId());
		}

		result.addTextList(dataPacket.getText());
		result.addDataList(dataPacket.getData());

		return false;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return readyToRun;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Accumulate a stream of incoming packets into a single combined packet. "
						+ "Use Collect to reassemble packets after a Split fan-out - "
						+ "it is the natural pair to Split.";
			}

			@Override
			public String expects() {
				return "Accepts: a stream of packets on a single input. "
						+ "In All mode, collects every packet until the stream ends then emits once. "
						+ "In ById mode, collects packets sharing the same ID and emits a combined packet "
						+ "each time the ID changes - input must be sorted by ID for this mode to work correctly.";
			}

			@Override
			public String produces() {
				return "Emits: a single packet whose text and data lists are the concatenation of all "
						+ "received packets. The output packet ID is set to the ID of the first received packet. "
						+ "In ById mode, one combined packet is emitted per distinct ID group.";
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
				return new CollectConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new CollectConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Collect";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		readyToRun = true;
	}

	@Override
	public boolean failed() {
		return false;
	}

}
