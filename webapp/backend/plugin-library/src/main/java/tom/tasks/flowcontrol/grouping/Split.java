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
public class Split extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private boolean failed;

	public Split() {
		input = null;
		failed = false;
	}

	public Split(TaskConfigSpec config) {
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
				return "Expand a single packet that contains multiple items in its text or data arrays "
						+ "into a stream of individual packets, one per item. "
						+ "Use Split to fan out a multi-record packet so each item can be processed "
						+ "individually downstream. Pair with Collect to reassemble.";
			}

			@Override
			public String expects() {
				return "Accepts: any packet whose text or data array contains multiple items. "
						+ "For each index position up to max(data.size(), text.size()), one output packet "
						+ "is produced containing the data item at that index (if present) and the text item "
						+ "at that index (if present).";
			}

			@Override
			public String produces() {
				return "Emits: a sequence of packets, one per item in the input text or data arrays. "
						+ "All output packets share the same ID as the input packet.";
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
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new NullTaskConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Split";
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
		return failed;
	}

}
