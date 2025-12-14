package tom.tasks.transform.renamer;

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

@RunnableTask
public class DataRenamer implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private DataRenamerConfig config;
	private Packet input;
	private Packet result;
	private String error;
	private boolean failed;

	public DataRenamer() {
		outputs = List.of();
		config = null;
		result = new Packet();
		error = null;
		input = null;
		failed = false;
	}

	public DataRenamer(DataRenamerConfig config) {
		this();
		this.config = config;
	}

	@Override
	public Packet getResult() {
		return null;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void run() {
		result = new Packet();
		result.setId(input.getId());
		List<Map<String, Object>> data = new ArrayList<>();
		Map<String, String> renames = config.getRenames();

		if (input == null) {
			outputs.get(0).write(result);
		}

		input.getData().forEach(item -> {
			item.forEach((key, value) -> {
				if (renames.containsKey(key)) {
					logger.debug("DataRenamer: Renaming " + key + " to " + renames.get(key));
					item.put(renames.get(key), value);
				} else {
					item.put(key, value);
				}
			});
		});

		result.setData(data);

		outputs.get(0).write(result);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"Workflow misconfiguration detect. DataRenamer should only ever have exactly one input!");
		}
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
			public String expects() {
				return "This task accepts any input. It inspects all input keys and remaps them per the given configuration. "
						+ "For example, if the configuration is\n{ \"A\": \"Aa\", \"B\": \"Bb\" }\nand the input "
						+ "is\n{ \"A\": \"Antelope\",\"C\": \"Cantaloupe\" }\nthen the result will be\n"
						+ " { \\\"Aa\\\": \\\"Antelope\\\",\\\"C\\\": \\\"Cantaloupe\\\" }";
			}

			@Override
			public String produces() {
				return "The remapped input data.";
			}

			@Override
			public int numOutputs() {
				// TODO Auto-generated method stub
				return 1;
			}

			@Override
			public int numInputs() {
				// TODO Auto-generated method stub
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new DataRenamerConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
				return new DataRenamerConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Rename Data Fields";
			}

			@Override
			public String group() {
				return "Transform";
			}

		};
	}

	@Override
	public void inputTerminated(int i) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean failed() {
		// TODO Auto-generated method stub
		return failed;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
