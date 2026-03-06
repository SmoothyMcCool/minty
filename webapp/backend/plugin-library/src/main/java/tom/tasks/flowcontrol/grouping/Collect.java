package tom.tasks.flowcontrol.grouping;

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
public class Collect implements MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet result;
	private Grouping grouping;
	private String groupingId;
	private boolean readyToRun;

	public Collect() {
		result = new Packet();
		readyToRun = false;
	}

	public Collect(CollectConfig config) {
		this();
		this.grouping = config.getGrouping();
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
		if (groupingId == null) {
			groupingId = dataPacket.getId();
			result.setId(dataPacket.getId());
		}

		result.addTextList(dataPacket.getText());
		result.addDataList(dataPacket.getData());

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
				return "Collect multiple packets into a single packet by appending contents together in a list.";
			}

			@Override
			public String expects() {
				return "Any packet.";
			}

			@Override
			public String produces() {
				return "A single packet containing a list of all packets received as input.";
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
				return new CollectConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new CollectConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Collect";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
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
		// this.logger = workflowLogger;
	}

}
