package tom.tasks.flowcontrol.grouping;

import java.util.HashMap;
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
import tom.tasks.GroupingEnumSpecCreator;
import tom.tasks.TaskGroup;

@RunnableTask
public class Flatten implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private StringBuilder sb;
	private boolean readyToRun;
	private Grouping grouping;
	private String separator;
	private String groupingId;
	private Packet result;

	public Flatten() {
		sb = new StringBuilder();
		readyToRun = false;
		grouping = Grouping.All;
		separator = "";
		groupingId = null;
		outputs = null;
		result = new Packet();
	}

	public Flatten(FlattenConfig config) {
		this();
		grouping = config.getGrouping();
		separator = config.getSeparator();
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
			output.write(result);
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
			logger.warn("Flatten: Workflow misconfiguration detect. Should only ever have exactly one input!");
		}

		if (groupingId == null) {
			groupingId = dataPacket.getId();
			result.setId(dataPacket.getId());
		}

		for (String str : dataPacket.getText()) {
			if (!sb.isEmpty()) {
				sb.append(separator);
			}
			sb.append(str);
		}
		result.setText(List.of(sb.toString()));

		Map<String, Object> dataResult = new HashMap<>();
		for (Map<String, Object> data : dataPacket.getData()) {
			dataResult.putAll(data);
		}
		result.setData(List.of(dataResult));

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
			public String description() {
				return "Flatten list data into a single data item.";
			}

			@Override
			public String expects() {
				return "Any packet. The input text and data arrays are combined into a single element. "
						+ "In the case of text, items are separated by \"Separator\". For data, maps are merged. "
						+ "In the case of key conflicts, later keys overwrite earlier keys.";
			}

			@Override
			public String produces() {
				return "For each input, one output packet containing the flattened text and data lists.";
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
				return new FlattenConfig(Map.of(GroupingEnumSpecCreator.EnumName, Grouping.All.toString()));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new FlattenConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Flatten";
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
