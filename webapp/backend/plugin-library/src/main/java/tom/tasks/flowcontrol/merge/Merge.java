package tom.tasks.flowcontrol.merge;

import java.util.ArrayList;
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
public class Merge implements MintyTask {

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
				return "Merge packets. One packet is taken from each input (if not completed), and merges the contents of the text and data arrays.";
			}

			@Override
			public String expects() {
				return "One packet on each input.";
			}

			@Override
			public String produces() {
				return "A packet that contains a unified list of text and data from each input.";
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

	@Override
	public void setLogger(TaskLogger workflowLogger) {
	}

}
