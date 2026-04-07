package tom.tasks.transform.formattext;

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
public class FormatText extends MintyTask {

	private List<? extends OutputPort> outputs;

	private FormatTextConfig config;
	private Packet input;
	private String error;
	private Packet result;
	private boolean failed;

	public FormatText() {
		input = null;
		outputs = null;
		error = null;
		failed = false;
	}

	public FormatText(FormatTextConfig config) {
		this();
		this.config = config;
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
		TemplateRenderer renderer = new TemplateRenderer();
		String rendered = renderer.render(input, config.getFormat());
		Packet result = new Packet(input);
		result.addTextList(List.of(rendered));

		outputs.get(0).write(result);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"FormatText: Workflow misconfiguration detect. FormatText should only ever have exactly one input!");
		}
		input = dataPacket;
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			warn("Workflow misconfiguration detect. FormatText should only ever have exactly one output!");
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
				return "Render a template string with values substituted from the input packet, "
						+ "appending the result to the output packet's text list. "
						+ "Use Format Text to construct a prompt before Query LLM, or to produce a "
						+ "formatted summary from structured data fields.";
			}

			@Override
			public String expects() {
				return "Accepts: any packet whose fields are referenced in the Format template. "
						+ "Placeholders use the syntax {path} — for example {data[0].name} or {text[0]}. "
						+ "Reference specific array indices to pull values from multi-record packets.";
			}

			@Override
			public String produces() {
				return "Emits: a copy of the input packet with one additional entry appended to the text list "
						+ "containing the rendered template string. Even if data contains multiple records, "
						+ "only one rendered string is produced per packet.";
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
				return new FormatTextConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new FormatTextConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Format Text";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
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

}
