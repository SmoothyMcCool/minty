package tom.tasks.transform.decorate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;
import tom.tasks.noop.NullTaskConfig;

/**
 * <p>
 * This task keeps the first packet it receives on input 0. Every subsequent
 * packet arriving on input 1 is merged with that “seed” packet and the
 * resulting packet is written to all connected outputs.
 * </p>
 *
 * <p>
 * The merge strategy is intentionally simple:
 * <ul>
 * <li>For both text and data, the output packet text and data lists are the two
 * inputs concatenated together.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The task completes when the upstream producer for input 1 has finished (i.e.
 * {@link #inputTerminated(int)} is called for input 1). At that point
 * {@link #readyToRun()} returns {@code true} and the framework will invoke
 * {@link #run()}. {@link #run()} processes all queued packets and emits the
 * merged result on every output port.
 * </p>
 */
@RunnableTask
public class Decorate extends MintyTask {

	private final List<OutputPort> outputs;
	private Packet seed; // packet from input 0
	private final Queue<Packet> incoming;
	boolean incomingInputTerminated;

	public Decorate() {
		outputs = new ArrayList<>();
		seed = null;
		incoming = new ArrayDeque<>();
		incomingInputTerminated = false;
	}

	public Decorate(TaskConfigSpec spec) {
		this();
	}

	@Override
	public Packet getResult() {
		// This task does not produce a single "final" result.
		return null;
	}

	@Override
	public String getError() {
		// No error handling implemented – return null.
		return null;
	}

	@Override
	public void run() {
		while (!incoming.isEmpty()) {
			Packet p = incoming.poll();
			Packet merged = mergeWithSeed(p);
			for (OutputPort out : outputs) {
				out.write(merged);
			}
		}
	}

	@Override
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		// Play games - on input 0, we will take the data here but then say we don't
		// want it, unless input 1 is terminated. That way we get the same packet
		// forever.
		if (inputNum == 0) {
			seed = dataPacket;
			return incomingInputTerminated;
		}
		return true;
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum == 0) {
			if (seed != null) {
				warn("Seed packet already set – ignoring new packet");
				return true; // keep the original seed
			}
			seed = dataPacket;
			debug("Seed packet received on input 0");
			return true;
		} else if (inputNum == 1) {
			incoming.offer(dataPacket);
			return true;
		}
		warn("Unknown input number: " + inputNum);
		return false;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs.clear();
		this.outputs.addAll(outputs);
	}

	@Override
	public boolean readyToRun() {
		// We are ready once the seed is set and the upstream for input 1 is finished.
		return seed != null && !incoming.isEmpty();
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Merges a persistent seed packet (input 0) with every packet "
						+ "received on input 1. The merged packet is " + "emitted to all connected outputs.";
			}

			@Override
			public String expects() {
				return "Accepts: any packet on input 0 (seed).  Any packet on input 1 "
						+ "(will be merged with the seed).";
			}

			@Override
			public String produces() {
				return "Emits: one merged packet per packet received on input 1, "
						+ "written to every connected output.";
			}

			@Override
			public int numOutputs() {
				// No configuration – default to 1 output
				return 1;
			}

			@Override
			public int numInputs() {
				return 2;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				// No configuration needed – return an empty spec
				return new NullTaskConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return taskConfiguration();
			}

			@Override
			public String taskName() {
				return "Decorate";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		if (i == 1) {
			incomingInputTerminated = true;
		}
	}

	@Override
	public boolean failed() {
		return false;
	}

	/**
	 * Merges the given packet with the seed packet.
	 *
	 * @param incoming the packet received on input 1
	 * @return a new packet containing the merged data
	 */
	private Packet mergeWithSeed(Packet incoming) {
		if (seed == null) {
			// Should never happen – guard just in case
			return incoming;
		}

		// Create a copy of the incoming packet so we do not mutate the original.
		Packet result = new Packet(incoming);

		// Merge text
		List<String> seedText = seed.getText();
		List<String> incomingText = incoming.getText();
		List<String> resultText = Stream.concat(seedText.stream(), incomingText.stream()).toList();
		result.setText(resultText);

		// Merge data
		List<Map<String, Object>> seedData = seed.getData();
		List<Map<String, Object>> incomingData = incoming.getData();
		List<Map<String, Object>> resultData = Stream.concat(seedData.stream(), incomingData.stream()).toList();
		result.setData(resultData);

		return result;
	}

}
