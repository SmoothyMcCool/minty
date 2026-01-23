package tom.tasks.flowcontrol;

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
public class Identify implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet input;
	private IdentifyConfig config;
	private boolean failed;

	public Identify() {
		input = null;
		config = null;
		failed = false;
	}

	public Identify(IdentifyConfig config) {
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
		Object keyValue = findKeyFromObject(config.getIdElement(), input.getData().getFirst());

		// current now holds the element that we want to use as the ID. If current is
		// null, we did not find the key we wanted.
		if (keyValue != null) {
			input.setId(keyValue.toString());
		} else {
			logger.warn("Identify: could not find the ID key in the provided data.");
		}

		for (OutputPort output : outputs) {
			output.write(input);
		}
	}

	private Object findKeyFromObject(String path, Object current) {
		String[] keys = path.split("\\.");

		Object o = current;

		for (String key : keys) {

			if (o instanceof Map) {
				o = ((Map<?, ?>) o).get(key);

			} else if (o instanceof List) {
				try {
					int index = Integer.parseInt(key);
					List<?> list = (List<?>) o;
					if (index < 0 || index >= list.size()) {
						o = null;
						break;
					}
					o = list.get(index);
				} catch (NumberFormatException e) {
					o = null;
					break;
				}

			} else {
				o = null;
				break;
			}

			if (o == null) {
				break;
			}
		}
		return o;
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
				return "Assign packet ID based onk the contents of a data field.";
			}

			@Override
			public String expects() {
				return "Any packet that contains data.";
			}

			@Override
			public String produces() {
				return "The same packet, with the identified data field extracted into the packet ID.";
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
				return "Identify";
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

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
