package tom.tasks.flowcontrol;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Identify extends MintyTask {

	private static final Pattern LIST_ACCESS_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)\\[(\\d+)]");

	private List<? extends OutputPort> outputs;

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
				return new IdentifyConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new IdentifyConfig(configuration);
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

	private static JsonNode resolvePath(JsonNode current, String path) {

		String[] parts = path.split("\\.", 2);
		String head = parts[0];
		String tail = (parts.length > 1) ? parts[1] : null;

		JsonNode next = null;

		Matcher listMatcher = LIST_ACCESS_PATTERN.matcher(head);

		if (listMatcher.matches()) {
			// format: field[index]
			String field = listMatcher.group(1);
			int index = Integer.parseInt(listMatcher.group(2));

			JsonNode arrayNode = current.get(field);
			if (arrayNode != null && arrayNode.isArray() && index >= 0 && index < arrayNode.size()) {
				next = arrayNode.get(index);
			} else {
				return null;
			}

		} else if (head.matches("\\d+")) {
			// format: .0 when current node is already an array
			int index = Integer.parseInt(head);

			if (current.isArray() && index >= 0 && index < current.size()) {
				next = current.get(index);
			} else {
				return null;
			}

		} else {
			// regular object field
			next = current.get(head);
			if (next == null) {
				return null;
			}
		}

		if (tail == null) {
			return next;
		}

		return resolvePath(next, tail);
	}

}
