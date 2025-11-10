package tom.tasks.flowcontrol;

import java.util.List;
import java.util.Map;

import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;

@RunnableTask
public class Identifier implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet input;
	private IdentifierConfig config;
	private boolean failed;

	public Identifier() {
		input = null;
		config = null;
		failed = false;
	}

	public Identifier(IdentifierConfig config) {
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
		Object keyValue = findKeyFromObject(config.getIdElement(), input.getData());

		// current now holds the element that we want to use as the ID. If current is
		// null, we did not find the key we wanted.
		if (keyValue != null) {
			input.setId(keyValue.toString());
		} else {
			logger.warn("Identifier: could not find the ID key in the provided data.");
		}

		for (OutputPort output : outputs) {
			output.write(input);
		}
	}

	private Object findKeyFromObject(String path, Object current) {
		String[] keys = path.split("\\.");

		for (String key : keys) {

			if (current instanceof Map) {
				current = ((Map<?, ?>) current).get(key);

			} else if (current instanceof List) {
				try {
					int index = Integer.parseInt(key);
					List<?> list = (List<?>) current;
					if (index < 0 || index >= list.size()) {
						current = null;
						break;
					}
					current = list.get(index);
				} catch (NumberFormatException e) {
					current = null;
					break;
				}

			} else {
				current = null;
				break;
			}

			if (current == null) {
				break;
			}
		}
		return current;
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
				return new JoinerConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new JoinerConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Identifier";
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
