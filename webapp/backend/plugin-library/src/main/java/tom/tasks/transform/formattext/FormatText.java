package tom.tasks.transform.formattext;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class FormatText implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private FormatTextConfig config;
	private Packet input;
	private String error;
	private Packet result;
	private boolean failed;

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");
	private static final Pattern LIST_ACCESS_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)\\[(\\d+)]");

	public FormatText() {
		input = null;
		outputs = null;
		error = null;
		failed = false;
	}

	public FormatText(FormatTextConfig config) {
		this();
		this.config = config;
	}

	@Override
	public Packet getResult() {
		return result;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void run() {

		Matcher matcher = PLACEHOLDER_PATTERN.matcher(config.getFormat());
		StringBuffer sb = new StringBuffer();

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		List<Map<String, Object>> dataList = input.getData();

		JsonNode root;
		try {
			// Wrap entire data array under "data"
			root = mapper.createObjectNode().set("data", mapper.valueToTree(dataList));
		} catch (Exception e) {
			logger.warn("FormatText: Could not parse input into JSON object.");
			return;
		}

		result = new Packet();
		result.setId(input.getId());
		result.setData(input.getData());

		while (matcher.find()) {
			String placeholder = matcher.group(0);
			String path = matcher.group(1).strip();

			if (path.equalsIgnoreCase("id")) {
				matcher.appendReplacement(sb, Matcher.quoteReplacement(input.getId()));
			} else if (path.equalsIgnoreCase("text")) {
				matcher.appendReplacement(sb,
						Matcher.quoteReplacement(String.join(System.lineSeparator(), input.getText())));
			} else {

				JsonNode valueNode = resolvePath(root, path);

				if (valueNode != null && !valueNode.isMissingNode() && !valueNode.isNull()) {
					matcher.appendReplacement(sb, Matcher.quoteReplacement(valueNode.asText()));
				} else {
					matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
				}
			}
		}

		matcher.appendTail(sb);

		result.addText(sb.toString());

		outputs.get(0).write(result);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"FormatText: Workflow misconfiguration detect. FormatText should only ever have exactly one input!");
		}
		input = dataPacket;
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn(
					"FormatText: Workflow misconfiguration detect. FormatText should only ever have exactly one output!");
		}
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
				return "Format Packet Data into Text.";
			}

			@Override
			public String expects() {
				return "This task produces text output based on the configuration provided. "
						+ "It scans for text in the form {path.to.json[1].element} and make appropriate "
						+ "substitutions from the input JSON object.";
			}

			@Override
			public String produces() {
				return "The provided config template with substitutions made from the received input. The result is returned in \"Text\".\n"
						+ "Even if data contains an array, the output \"Text\" element will contain only one element, but substitutions can refer to different array elements.";
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
				return new FormatTextConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new FormatTextConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Format Text";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
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

		String[] parts = path.split("\\.", 2); // head + remainder
		String head = parts[0];
		String tail = (parts.length > 1) ? parts[1] : null;

		JsonNode next = null;

		Matcher listMatcher = LIST_ACCESS_PATTERN.matcher(head);
		if (listMatcher.matches()) {
			// List access, e.g. friends[0]
			String field = listMatcher.group(1);
			int index = Integer.parseInt(listMatcher.group(2));

			JsonNode arrayNode = current.get(field);
			if (arrayNode != null && arrayNode.isArray() && index >= 0 && index < arrayNode.size()) {
				next = arrayNode.get(index);
			} else {
				return null;
			}

		} else {
			// Regular map access
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

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
