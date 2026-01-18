package tom.tasks.flowcontrol.packetlog;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

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
public class LogPacket implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private Packet input;
	private boolean failed;

	public LogPacket() {
		input = null;
		failed = false;
	}

	public LogPacket(TaskConfigSpec config) {
		this();
	}

	@Override
	public Packet getResult() {
		return input;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void run() {
		try {
			logger.info(input.toJson());
		} catch (JsonProcessingException e) {
			logger.info(input.toString());
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
				return "Log contents of packets to the Workflow log, for debugging.";
			}

			@Override
			public String expects() {
				return "Any data packet.";
			}

			@Override
			public String produces() {
				return "The same packet, after logging it to the workflow run log.";
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
				return new NullTaskConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new NullTaskConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Packet Logger";
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

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
