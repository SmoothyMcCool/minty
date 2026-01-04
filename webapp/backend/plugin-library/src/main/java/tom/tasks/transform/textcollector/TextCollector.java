package tom.tasks.transform.textcollector;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.Grouping;
import tom.tasks.TaskGroup;

@RunnableTask
public class TextCollector implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private StringBuilder result;
	private boolean readyToRun;
	private Grouping grouping;
	private String separator;
	private String groupingId;

	public TextCollector() {
		result = new StringBuilder();
		readyToRun = false;
		grouping = Grouping.All;
		separator = "";
		groupingId = null;
		outputs = null;
	}

	public TextCollector(TextCollectorConfig config) {
		this();
		grouping = config.getGrouping();
		separator = config.getSeparator();
	}

	@Override
	public Packet getResult() {
		Packet packet = new Packet();
		packet.addText(result.toString());
		return packet;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void run() {
		Packet results = new Packet();
		results.setId(groupingId);
		results.addText(result.toString());
		for (OutputPort output : outputs) {
			output.write(results);
		}
	}

	@Override
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		if (grouping == Grouping.ById) {
			if (groupingId == null) {
				return true;
			}
			if (!groupingId.equals(dataPacket.getId())) {
				readyToRun = true;
			}
			return groupingId.equals(dataPacket.getId());
		}

		return true;
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			logger.warn(
					"TextCollector: Workflow misconfiguration detect. TextCollector should only ever have exactly one input!");
		}

		if (groupingId == null) {
			groupingId = dataPacket.getId();
		}

		for (String str : dataPacket.getText()) {
			if (!result.isEmpty()) {
				result.append(separator);
			}
			result.append(str);
		}
		return false;
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
			public String expects() {
				return "This task collects the Text elements of Packets it receives and joins them together. "
						+ "It will either group all records received into a single output, or can be configured "
						+ "to group elements of the same key.";
			}

			@Override
			public String produces() {
				return "This task produces no output. The collected text is available as a result.";
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
				return new TextCollectorConfig(Map.of(TextCollectorConfig.GroupingLabel, Grouping.All.toString()));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new TextCollectorConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Text Collector";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		readyToRun = true;
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
