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
public class Funnel extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private FunnelConfig config;

	public Funnel() {
		input = null;
		config = null;
	}

	public Funnel(FunnelConfig config) {
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
		debug("received packet of Id " + dataPacket.getId() + ", on port " + inputNum);
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
				return "Route packets from multiple inputs to a single output, passing each through unchanged. "
						+ "Use Funnel to reconnect branches separated by a Broadcast into one downstream step.";
			}

			@Override
			public String expects() {
				return "Accepts: any packets on any connected input. Packets are forwarded individually "
						+ "as they arrive — there is no waiting, buffering, or combining across inputs.";
			}

			@Override
			public String produces() {
				return "Emits: each received packet forwarded unchanged to the single output. "
						+ "To combine the contents of one packet from each input into one, use Merge instead.";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return config != null ? config.getNumInputs() : 2;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new FunnelConfig(Map.of(FunnelConfig.NumInputs, "2"));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new FunnelConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Funnel";
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
