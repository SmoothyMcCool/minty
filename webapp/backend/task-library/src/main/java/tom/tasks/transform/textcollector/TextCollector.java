package tom.tasks.transform.textcollector;

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
public class TextCollector implements MintyTask {

	private TaskLogger logger;
	private String result;
	private boolean inputCollected;

	public TextCollector() {
		result = "";
		inputCollected = false;
	}

	public TextCollector(TextCollectorConfig config) {
		this();
	}

	@Override
	public Packet getResult() {
		Packet packet = new Packet();
		packet.addText(result);
		return packet;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void run() {
		// Doesn't have to do anything!
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			logger.warn(
					"TextCollector: Workflow misconfiguration detect. TextCollector should only ever have exactly one input!");
		}
		result = result + "\n\n" + dataPacket.getText().toString();
		return inputCollected;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 0) {
			logger.warn("TextCollector: Workflow misconfiguration detect. TextCollector should have no outputs!");
		}
	}

	@Override
	public boolean readyToRun() {
		return inputCollected;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String expects() {
				return "This task collects the Text elements of Packets it receives and joins them together.";
			}

			@Override
			public String produces() {
				return "This task produces no output. The collected text is available as a result.";
			}

			@Override
			public int numOutputs() {
				return 0;
			}

			@Override
			public int numInputs() {
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new TextCollectorConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new TextCollectorConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Text Collector";
			}

			@Override
			public String group() {
				return "Transformer";
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		inputCollected = true;
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
