package tom.tasks.flowcontrol.branching;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Broadcast extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private BroadcastConfig config;

	public Broadcast() {
		input = null;
		config = null;
	}

	public Broadcast(BroadcastConfig config) {
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
			public String description() {
				return "Copy each incoming packet identically to all connected outputs. "
						+ "Use Broadcast when the same data needs to flow into multiple independent "
						+ "processing branches in parallel.";
			}

			@Override
			public String expects() {
				return "Accepts: any packet. Each received packet is copied to every connected output.";
			}

			@Override
			public String produces() {
				return "Emits: an identical copy of the input packet on every connected output. "
						+ "Data is not divided or modified — all outputs receive the same content.";
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
				return new BroadcastConfig(Map.of(BroadcastConfig.NumOutputs, "2"));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new BroadcastConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Broadcast";
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
		return false;
	}

}
