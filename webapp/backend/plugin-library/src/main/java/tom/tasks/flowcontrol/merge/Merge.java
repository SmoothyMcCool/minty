package tom.tasks.flowcontrol.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Merge extends MintyTask {

	private static final Packet NullPacket = new Packet();

	private List<? extends OutputPort> outputs;

	private List<Packet> inputs;

	public Merge() {
		inputs = null;
	}

	public Merge(MergeConfig config) {
		this();
		int numInputs = config.getNumInputs();
		inputs = new ArrayList<>();
		for (int i = 0; i < numInputs; i++) {
			inputs.add(null);
		}
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
		Packet result = new Packet();
		// Take the ID of the first non-null packet
		String id = inputs.stream().filter(input -> input != NullPacket).findFirst().orElse(NullPacket).getId();
		result.setId(id);

		List<String> text = new ArrayList<>();
		for (Packet input : inputs) {
			text.addAll(input.getText());
		}
		result.setText(text);

		List<Map<String, Object>> data = new ArrayList<>();
		for (Packet input : inputs) {
			data.addAll(input.getData());
		}
		result.setData(data);

		for (OutputPort output : outputs) {
			output.write(result);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		inputs.set(inputNum, dataPacket);
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return inputs.stream().anyMatch(input -> input == null);
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Wait for one packet from each input, then combine their text and data arrays "
						+ "into a single output packet. Use Merge to reunite parallel branches that each "
						+ "produce one packet - for example after a Broadcast where different branches "
						+ "transform the same data.";
			}

			@Override
			public String expects() {
				return "Accepts: exactly one packet per input. Merge is synchronised - it waits until "
						+ "every connected input has delivered a packet before running. If an upstream branch "
						+ "completes without sending a packet, that input contributes empty text and data.";
			}

			@Override
			public String produces() {
				return "Emits: a single packet whose text list is the concatenation of all input text lists, "
						+ "and whose data list is the concatenation of all input data lists. "
						+ "The output packet ID is taken from the first non-empty input.";
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
				return new MergeConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new MergeConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Merge";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		inputs.set(i, NullPacket);
	}

	@Override
	public boolean failed() {
		return false;
	}

}
