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
public class Splitter implements MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private SplitterConfig config;

	public Splitter() {
		input = null;
		config = null;
	}

	public Splitter(SplitterConfig config) {
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
			public String expects() {
				return "Sends all received input on all connected outputs.";
			}

			@Override
			public String produces() {
				return "Each input is sent unmodified to each connected output.";
			}

			@Override
			public int numOutputs() {
				return config != null ? config.getNumOutputs() : 2;
			}

			@Override
			public int numInputs() {
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new SplitterConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new SplitterConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Splitter";
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
		return false;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
	}

}
