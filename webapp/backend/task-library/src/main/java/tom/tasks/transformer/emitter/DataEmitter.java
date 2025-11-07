package tom.tasks.transformer.emitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.TaskSpec;
import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.annotation.RunnableTask;

@RunnableTask
public class DataEmitter implements MintyTask {

	private static final Logger logger = LogManager.getLogger(DataEmitter.class);

	private DataEmitterConfig config;
	private List<? extends OutputPort> outputs;
	private boolean readyToRun;

	public DataEmitter() {
		outputs = new ArrayList<>();
		readyToRun = true; // Starts as true since this task takes no input.
	}

	public DataEmitter(DataEmitterConfig config) {
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
		// This task only runs once, so flip readyToRun to false.
		readyToRun = false;

		List<String> data = config.getData();
		String key = config.getKeyName();
		ObjectMapper mapper = new ObjectMapper();

		data.forEach(datum -> {
			Packet packet = new Packet();
			Map<String, Object> packetData = new HashMap<>();

			Object value = null;
			try {
				value = mapper.readValue(datum, Map.class);
				packetData.put(key, value);
				packet.setData(packetData);
				outputs.get(0).write(packet);
				logger.info("key: " + key + ", value: " + value);

			} catch (JsonProcessingException e) {
				logger.warn("Failed to translate " + datum + " to valid JSON: ", e);
			}
		});

	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		// This task should never receive input. If we ever do, log the error, but
		// signal that we have all the input we need.
		logger.warn("Workflow misconfiguration detect. Record Emitter should never receive input!");
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
				return "This task does not receive any data. It runs once when the workflow starts, emitting records as specified.";
			}

			@Override
			public String produces() {
				return "An array of the items set in the configuration.";
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
				return new DataEmitterConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new DataEmitterConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Record Emitter";
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
		// This task can't fail.
		return false;
	}

}
