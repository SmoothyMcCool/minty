package tom.tasks.transform.pipeline;

import java.util.List;
import java.util.Map;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;
import tom.tasks.transform.pipeline.model.PipelineDefinition;
import tom.tasks.transform.pipeline.operations.PipelineOperationRegistry;
import tom.tasks.transform.pipeline.operations.TransformOperation;

@RunnableTask
public class PipelineTransform implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private PipelineTransformConfig config;
	private Packet input;
	private Packet result;
	private String error;
	private boolean failed;

	public PipelineTransform() {
		outputs = List.of();
		config = null;
		result = new Packet();
		error = null;
		input = null;
		failed = false;
	}

	public PipelineTransform(PipelineTransformConfig config) {
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
		try {
			PipelineDefinition pipeline = config.getPipelineDefinition();

			// Clone input so we don't mutate workflow state unexpectedly
			result = new Packet(input);

			String prePipeline = input.toJson();

			for (var operation : pipeline.getOperations()) {

				String opName = operation.getName();
				TransformOperation op = PipelineOperationRegistry.get(opName);

				if (op == null) {
					throw new RuntimeException("Unknown pipeline operation: " + opName);
				}

				logger.debug("Applying pipeline operation: " + opName);
				// Execute operation
				op.execute(result, operation.getConfiguration());

			}

			String postPipeline = result.toJson();

			logger.debug("Pipeline transform completed");
			logger.debug("Before: " + prePipeline);
			logger.debug("After: " + postPipeline);

			outputs.get(0).write(result);

		} catch (Exception e) {
			failed = true;
			error = "PipelineTransform failed: " + e.getMessage();
			logger.error(error, e);
			throw new RuntimeException(error, e);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"Workflow misconfiguration detect. PipelineTransform should only ever have exactly one input!");
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
			public String description() {
				return "Transform contents of Packet data.";
			}

			@Override
			public String expects() {
				return "This task accepts any input. It performs a series of predefined transformations on each received packet.";
			}

			@Override
			public String produces() {
				return "The transformed data.";
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
				return new PipelineTransformConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new PipelineTransformConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Pipeline Transform";
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

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}
}
