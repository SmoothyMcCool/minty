package tom.tasks.emit.document;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class DocumentEmitter implements MintyTask, ServiceConsumer {

	private TaskLogger logger;
	private DocumentEmitterConfig config;
	private List<? extends OutputPort> outputs;
	private boolean readyToRun;
	private boolean failed;
	private PluginServices pluginServices;

	public DocumentEmitter() {
		outputs = new ArrayList<>();
		readyToRun = true; // Starts as true since this task takes no input.
		failed = false;
		pluginServices = null;
	}

	public DocumentEmitter(DocumentEmitterConfig config) {
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
		byte[] data = Base64.getDecoder().decode(config.getBase64());
		String text = pluginServices.getDocumentService().fileBytesToText(data);

		Packet p = new Packet();
		p.setId(null);
		p.addText(text);
		p.setData(null);

		outputs.get(0).write(p);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		// This task should never receive input. If we ever do, log the error, but
		// signal that we have all the input we need.
		logger.warn("Workflow misconfiguration detect. Document Emitter should never receive input!");
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		if (outputs.size() != 1) {
			logger.warn("Workflow misconfiguration detect. Document Emitter should only ever have exactly one output!");
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
				return "Read a document and emit its contents as a packet for further processing.";
			}

			@Override
			public String expects() {
				return "This task does not receive any data. It runs once when the workflow starts, emitting the contents of the specified file.";
			}

			@Override
			public String produces() {
				return "A single packet with null ID and null data. Text contains a single string - the file contents.";
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
				return new DocumentEmitterConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new DocumentEmitterConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Document Emitter";
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

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

}
