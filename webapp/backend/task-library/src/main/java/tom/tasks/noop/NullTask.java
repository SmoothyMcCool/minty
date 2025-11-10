package tom.tasks.noop;

import java.util.List;
import java.util.Map;

import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;

public class NullTask implements MintyTask {

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void run() {
		// Do nothing.
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		// Nothing to do.
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
	}

	@Override
	public boolean readyToRun() {
		return true;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String expects() {
				return "This task expects no input. It doesn't matter what you provide.";
			}

			@Override
			public String produces() {
				return "This task produces no output. Using it will cause your workflow to stop running.";
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
				return new NullTaskConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return taskConfiguration();
			}

			@Override
			public String taskName() {
				return "Null Task";
			}

			@Override
			public String group() {
				// TODO Auto-generated method stub
				return "Transformer";
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
	public Packet getResult() {
		return null;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
	}

}
