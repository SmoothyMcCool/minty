package tom.tasks.flowcontrol.grouping;

import java.util.List;
import java.util.Map;

import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;
import tom.tasks.flowcontrol.JoinerConfig;

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
			logger.debug("GroupBy: Writing out " + result.getDataList().size() + " items for key " + keyPacket.getId());
			output.write(result);
		}
	}

	@Override
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0 && keyPacket == null) {
			return true;
		}
		if (inputNum == 1 && keyPacket.getId().equals(dataPacket.getId())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0) {
			if (keyPacket == null) {
				keyPacket = dataPacket;
			} else {
				throw new RuntimeException("GroupBy: key Packet already received!");
			}
			return true;
		}
		if (inputNum == 1) {
			if (keyPacket.getId().equals(dataPacket.getId())) {
				result.addDataList(dataPacket.getDataList());
				return false;
			}
			readyToRun = true;
			return true;
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
				return new JoinerConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new JoinerConfig(configuration);
			}

			@Override
			public String taskName() {
				return "GroupBy";
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
