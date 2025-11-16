package tom.tasks.transform.emitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;

@RunnableTask
public class PacketEmitter implements MintyTask {

	private TaskLogger logger;
	private PacketEmitterConfig config;
	private List<? extends OutputPort> outputs;
	private boolean readyToRun;
	private boolean failed;

	public PacketEmitter() {
		outputs = new ArrayList<>();
		readyToRun = true; // Starts as true since this task takes no input.
		failed = false;
	}

	public PacketEmitter(PacketEmitterConfig config) {
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
		String rawData = config.getData();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		List<Packet> data = new ArrayList<>();
		try {
			JsonNode root = mapper.readTree(rawData);

			for (JsonNode node : root) {
				try {
					Packet p = mapper.treeToValue(node, Packet.class);
					data.add(p);
				} catch (Exception e) {
					failed = true;
					throw new PacketEmitterException(
							"PacketEmitter: Contains malformed data. Record is: " + node.toString(), e);
				}
			}

		} catch (Exception e) {
			failed = true;
			logger.warn("PacketEmitter: Data is not valid JSON list.");
			throw new RuntimeException(e);
		}

		for (Packet p : data) {
			logger.debug("PacketEmitter: Emitting " + p.toString());
			outputs.get(0).write(p);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		// This task should never receive input. If we ever do, log the error, but
		// signal that we have all the input we need.
		logger.warn("Workflow misconfiguration detect. Packet Emitter should never receive input!");
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn("Workflow misconfiguration detect. Record Emitter should only ever have exactly one output!");
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
			public String expects() {
				return "This task does not receive any data. It runs once when the workflow starts, emitting Packet as specified.";
			}

			@Override
			public String produces() {
				return "An array of the items set in the configuration. Format the data as follows: [ { \"id\": \"string ID\", \"text\": \"Any text\", \"data\": [ {arbitrary JSON data} ] ]";
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
				return new PacketEmitterConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new PacketEmitterConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Packet Emitter";
			}

			@Override
			public String group() {
				return "Producer";
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
