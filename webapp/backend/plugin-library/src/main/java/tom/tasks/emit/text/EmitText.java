package tom.tasks.emit.text;

import java.util.ArrayList;
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

@RunnableTask
public class EmitText implements MintyTask {
	private TaskLogger logger;
	private EmitTextConfig config;
	private List<? extends OutputPort> outputs;
	private boolean readyToRun;
	private boolean failed;

	public EmitText() {
		outputs = new ArrayList<>();
		readyToRun = true; // Starts as true since this task takes no input.
		failed = false;
	}

	public EmitText(EmitTextConfig config) {
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
		String text = config.getText();

		Packet output = new Packet();
		output.setId("");
		output.setData(List.of());
		output.setText(List.of(text));

		logger.debug("EmitText: Emitting " + output.toString());
		outputs.get(0).write(output);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		// This task should never receive input. If we ever do, log the error, but
		// signal that we have all the input we need.
		logger.warn("Workflow misconfiguration detect. Text Emitter should never receive input!");
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn("Workflow misconfiguration detect. Text Emitter should only ever have exactly one output!");
		}
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
				return "Emit Text. A simpler version of the Packet Emitter.";
			}

			@Override
			public String expects() {
				return "This task does not receive any data. It runs once when the workflow starts, emitting a single Packet containing the given text.";
			}

			@Override
			public String produces() {
				return "A single packet, formatted as follows: { \"id\": \"\", \"text\": \"{user-supplied text}\", \"data\": [] }";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return 0;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new EmitTextConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new EmitTextConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Emit Text";
			}

			@Override
			public String group() {
				return TaskGroup.EMIT.toString();
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
