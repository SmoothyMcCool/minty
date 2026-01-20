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
import tom.tasks.TaskGroup;
import tom.tasks.noop.NullTaskConfig;

@RunnableTask
public class GroupBy implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet keyPacket;
	private Packet result;
	private boolean failed;
	private boolean readyToRun;

	public GroupBy() {
		keyPacket = null;
		result = new Packet();
		failed = false;
		readyToRun = false;
	}

	public GroupBy(TaskConfigSpec config) {
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
			logger.debug("GroupBy: Writing out items for key " + keyPacket.getId());
			output.write(result);
		}
	}

	@Override
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0 && keyPacket == null) {
			readyToRun = false;
			return true;
		}
		if (inputNum == 1 && keyPacket != null && keyPacket.getId().equals(dataPacket.getId())) {
			readyToRun = false;
			return true;
		}
		if (keyPacket == null) {
			throw new RuntimeException("Input received without having received a key packet. Is your input sorted?");
		}
		readyToRun = true;
		if (inputNum == 1) {
			logger.info("GroupBy: Rejecting packet with id " + dataPacket.getId());
		}

		return false;
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0) {
			if (keyPacket == null) {
				logger.debug("GroupBy: Setting key with Id " + dataPacket.getId());
				keyPacket = dataPacket;
				result.setId(dataPacket.getId());
			} else {
				throw new RuntimeException("GroupBy: key Packet already received!");
			}
			return true;
		}
		if (inputNum == 1) {
			logger.debug("GroupBy: Adding packet with Id " + dataPacket.getId());
			result.addDataList(dataPacket.getData());
			result.addTextList(dataPacket.getText());
			return false;
		}
		failed = true;
		throw new RuntimeException(
				"Workflow misconfiguration detect. GroupBy should only ever have exactly two inputs!");
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
				return "Match up Packets based on ID and combine them into one.";
			}

			@Override
			public String expects() {
				return "One packet on input 1, multiple sorted packets on input two. For each packet on input two, where IDs match, they are grouped into a single output packet.";
			}

			@Override
			public String produces() {
				return "For each input on port 2 that matches the id of the packet received on port 1, it is aggregated into a single output packet with a list of the data.";
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
				return "GroupBy";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		if (keyPacket != null && i == 1) {
			readyToRun = true;
		}
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
