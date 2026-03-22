package tom.tasks.flowcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Sort extends MintyTask {

	private List<? extends OutputPort> outputs;

	private List<Packet> input;
	private boolean allInputReceived;
	private boolean failed;
	private List<String> sortElements;

	public Sort() {
		input = new ArrayList<>();
		allInputReceived = false;
		failed = false;
	}

	public Sort(SortConfig config) {
		this();
		sortElements = config.getIdElement();
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
		input.sort((left, right) -> {
			for (String element : sortElements) {
				int result = left.resolve(element).compareTo(right.resolve(element));
				if (result != 0) {
					return result;
				}
			}
			return 0;
		});

		for (OutputPort output : outputs) {
			for (Packet p : input) {
				debug("Sending packet with Id: " + p.getId());
				output.write(p);
			}
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		debug("Received packet of ID " + dataPacket.getId());
		input.add(dataPacket);
		return false;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return allInputReceived;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Sort Packets by ID.";
			}

			@Override
			public String expects() {
				return "Any packets. This task only runs once, and it will only run once it has received all input from the previous step.";
			}

			@Override
			public String produces() {
				return "Each input is sent unmodified to the output, sorted by ID.";
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
				return new SortConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				try {
					return new SortConfig(configuration);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Failed to read configuration.", e);
				}
			}

			@Override
			public String taskName() {
				return "Sort";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		allInputReceived = true;
	}

	@Override
	public boolean failed() {
		return failed;
	}

}
