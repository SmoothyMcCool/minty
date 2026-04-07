package tom.tasks.flowcontrol.grouping;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;
import tom.tasks.noop.NullTaskConfig;

@RunnableTask
public class GroupBy extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet keyPacket;
	private Packet result;
	private boolean failed;
	private boolean readyToRun;

	public GroupBy() {
		keyPacket = null;
		result = new Packet();
		failed = false;
		readyToRun = false;
	}

	public GroupBy(TaskConfigSpec config) {
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
			debug("Writing out items for key " + keyPacket.getId());
			output.write(result);
		}
	}

	@Override
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0 && keyPacket == null) {
			readyToRun = false;
			return true;
		}
		if (inputNum == 1 && keyPacket != null && keyPacket.getId().equals(dataPacket.getId())) {
			readyToRun = false;
			return true;
		}
		if (keyPacket == null) {
			throw new RuntimeException("Input received without having received a key packet. Is your input sorted?");
		}
		readyToRun = true;
		if (inputNum == 1) {
			info("Rejecting packet with id " + dataPacket.getId());
		}

		return false;
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0) {
			if (keyPacket == null) {
				debug("Setting key with Id " + dataPacket.getId());
				keyPacket = dataPacket;
				result.setId(dataPacket.getId());
			} else {
				throw new RuntimeException("GroupBy: key Packet already received!");
			}
			return true;
		}
		if (inputNum == 1) {
			debug("Adding packet with Id " + dataPacket.getId());
			result.addDataList(dataPacket.getData());
			result.addTextList(dataPacket.getText());
			return false;
		}
		failed = true;
		throw new RuntimeException(
				"Workflow misconfiguration detect. GroupBy should only ever have exactly two inputs!");
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
				return "Join two sorted streams on a shared packet ID, grouping matching detail records "
						+ "under each key. Input 0 receives key packets one at a time; "
						+ "input 1 receives a sorted stream of detail packets. "
						+ "For each key, all matching detail packets are combined into one output packet.";
			}

			@Override
			public String expects() {
				return "Accepts: input 0 — a single key packet whose ID defines the grouping key. "
						+ "Input 1 — a stream of detail packets sorted by ID. "
						+ "Detail packets whose ID matches the current key are accumulated; "
						+ "when the ID changes the result is emitted and the next key packet is read. "
						+ "Both streams must be sorted by ID — add Sort and SetId steps upstream if needed.";
			}

			@Override
			public String produces() {
				return "Emits: one output packet per key, containing the key packet's ID and the "
						+ "concatenated text and data from all matching detail packets.";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return 2;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new NullTaskConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new NullTaskConfig(configuration);
			}

			@Override
			public String taskName() {
				return "GroupBy";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		if (keyPacket != null && i == 1) {
			readyToRun = true;
		}
	}

	@Override
	public boolean failed() {
		return failed;
	}

}
