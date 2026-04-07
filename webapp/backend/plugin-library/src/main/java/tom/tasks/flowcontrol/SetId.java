package tom.tasks.flowcontrol;

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
public class SetId extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private SetIdConfig config;
	private boolean failed;

	public SetId() {
		input = null;
		config = null;
		failed = false;
	}

	public SetId(SetIdConfig config) {
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
		String idPath = config.getIdElement();
		String value = input.resolve(idPath);

		if (value != null) {
			input.setId(value);
		} else {
			warn("Could not find ID element: " + idPath);
		}

		for (OutputPort output : outputs) {
			output.write(input);
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
				return "Set the packet ID by extracting a value from a data field or text element. "
						+ "Use SetId to assign a meaningful key before steps that operate on packet IDs "
						+ "— Sort, GroupBy, and Collect in ById mode all use the packet ID.";
			}

			@Override
			public String expects() {
				return "Accepts: any packet containing the field referenced by the ID Element configuration. "
						+ "The path uses dot notation, e.g. data[0].customerId. "
						+ "If the path cannot be resolved a warning is logged and the ID is left unchanged.";
			}

			@Override
			public String produces() {
				return "Emits: the same packet with its ID field updated to the value found at the "
						+ "configured path. All other fields are passed through unchanged.";
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
				return new SetIdConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new SetIdConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Set Id";
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
