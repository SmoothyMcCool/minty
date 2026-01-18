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
import tom.tasks.noop.NullTaskConfig;

@RunnableTask
public class Merge implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet[] input;

	public Merge() {
		input = new Packet[2];
	}

	public Merge(TaskConfigSpec config) {
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
		Packet result = new Packet();
		result.setId(input[0].getId() + input[1].getId());

		List<String> text = new ArrayList<>();
		if (input[0].getText() != null) {
			text.addAll(input[0].getText());
		}
		if (input[1].getText() != null) {
			text.addAll(input[1].getText());
		}
		result.setText(text);

		List<Map<String, Object>> data = new ArrayList<>();
		if (input[0].getData() != null) {
			data.addAll(input[0].getData());
		}
		if (input[1].getText() != null) {
			data.addAll(input[1].getData());
		}
		result.setData(data);

		for (OutputPort output : outputs) {
			output.write(result);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		input[inputNum] = dataPacket;
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
				return new NullTaskConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new NullTaskConfig(configuration);
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
