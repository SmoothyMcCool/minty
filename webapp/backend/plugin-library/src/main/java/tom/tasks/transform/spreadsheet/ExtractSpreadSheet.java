package tom.tasks.transform.spreadsheet;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;
import tom.tasks.noop.NullTaskConfig;

@RunnableTask
public class ExtractSpreadSheet extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private boolean failed;
	private String error;
	private Packet result;

	public ExtractSpreadSheet() {
		input = null;
		outputs = null;
		failed = false;
		error = null;
		result = null;
	}

	public ExtractSpreadSheet(TaskConfigSpec config) {
		this();
	}

	@Override
	public Packet getResult() {
		return result;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void run() {
		Packet result = new Packet();
		input.getText().stream().map(text -> TsvExtractor.parse(text)).flatMap(List::stream)
				.forEach(packet -> result.getData().addAll(packet.getData()));
		result.setId(input.getId());
		outputs.get(0).write(result);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"ExtractSpreadSheet: Workflow misconfiguration detect. ExtractSpreadSheet should only ever have exactly one input!");
		}

		input = dataPacket;

		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			warn("Workflow misconfiguration detect. ExtractSpreadSheet should only ever have exactly one output!");
		}
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
				return "Parse TSV (tab-separated values) text from the input packet into structured data records. "
						+ "Pair with Emit Document to process uploaded spreadsheets: Emit Document converts the file "
						+ "to Markdown containing a TSV table, then Read TSV Data turns those rows into data records.";
			}

			@Override
			public String expects() {
				return "Accepts: a packet with one or more items in the text list, each containing TSV-formatted "
						+ "content. The first row of each TSV block is the header row - its values become the "
						+ "field names in the output data records.";
			}

			@Override
			public String produces() {
				return "Emits: a single packet whose data list contains one record per non-header row across "
						+ "all input text items. Each record is a map of column header to cell value. "
						+ "The output packet ID is copied from the input.";
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
				return "Read TSV Data";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public String html() {
		return "";
	}

}
