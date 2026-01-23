package tom.tasks.emit.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class EmitPacket implements MintyTask {

	private TaskLogger logger;
	private EmitPacketConfig config;
	private List<? extends OutputPort> outputs;
	private boolean readyToRun;
	private boolean failed;

	public EmitPacket() {
		outputs = new ArrayList<>();
		readyToRun = true; // Starts as true since this task takes no input.
		failed = false;
	}

	public EmitPacket(EmitPacketConfig config) {
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
			List<Packet> packets = mapper.readValue(rawData, new TypeReference<List<Packet>>() {
			});

			for (Packet packet : packets) {
				data.add(packet);
			}

		} catch (Exception e) {
			failed = true;
			logger.warn("EmitPacket: Data is not valid JSON list.");
			throw new RuntimeException(e);
		}

		for (Packet p : data) {
			logger.debug("EmitPacket: Emitting " + p.toString());
			outputs.get(0).write(p);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		// This task should never receive input. If we ever do, log the error, but
		// signal that we have all the input we need.
		logger.warn("Workflow misconfiguration detect. EmitPacket should never receive input!");
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn("Workflow misconfiguration detect. EmitPacket should only ever have exactly one output!");
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
				return "Emit packets, mostly to control the start state of a workflow.";
			}

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
				return new EmitPacketConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new EmitPacketConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Emit Packet";
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
