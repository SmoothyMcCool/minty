package tom.tasks.flowcontrol.conditional;

import java.util.List;
import java.util.Map;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Loop implements MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private ConditionalConfig config;
	private TaskLogger logger;
	private boolean terminalFailure;
	private boolean loopComplete;

	public Loop() {
		input = null;
		config = null;
		terminalFailure = false;
		loopComplete = false;
	}

	public Loop(ConditionalConfig config) {
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
		String expression = config.getBranchExpression();
		// Turn data.foo -> data[0].foo, same for text
		expression = expression.replaceAll("\\bdata\\.(\\w+)", "data[0].$1");
		expression = expression.replaceAll("\\btext\\.(\\w+)", "text[0].$1");

		try {

			// Evaluate with SpEL
			Packet result = input;
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext context = new StandardEvaluationContext(result);
			context.setBeanResolver(null);

			Object raw = parser.parseExpression(expression).getValue(context);
			if (!(raw instanceof Boolean)) {
				throw new IllegalArgumentException("Expression did not return a boolean value: " + raw);
			}
			loopComplete = !((Boolean) raw);
			int outputPort = loopComplete ? 1 : 0;
			outputs.get(outputPort).write(result);

		} catch (Exception e) {
			String error = "Loop caught exception while trying to apply SpEL expression: " + expression + ", to input: "
					+ input.toString();
			logger.error(error);
			terminalFailure = true;
			throw new RuntimeException(error, e);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
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
				return "Evaluate a condition. If the condition is not met, packet is emitted on output 0. If it is met, it is emitted on output 1, and the loop terminates. If you want to evaluate a conditoin without looping, use Branch instead.";
			}

			@Override
			public String expects() {
				return "Any packet, against which an expression is evaluated to determine the output port.";
			}

			@Override
			public String produces() {
				return "Each input is sent unmodified to output 0 if the condition evaluates to true, or 1 if the condition is false. Once a packet is sent on output 1, the task terminates.";
			}

			@Override
			public int numOutputs() {
				return 2;
			}

			@Override
			public int numInputs() {
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new ConditionalConfig(Map.of(ConditionalConfig.BranchExpression, ""));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new ConditionalConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Loop";
			}

			@Override
			public String group() {
				return TaskGroup.FLOW_CONTROL.toString();
			}
		};
	}

	@Override
	public void inputTerminated(int i) {
		// Nothing to do.
	}

	@Override
	public boolean failed() {
		return false;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		logger = workflowLogger;
	}

	@Override
	public boolean terminalFailure() {
		return terminalFailure;
	}

	@Override
	public boolean stepComplete() {
		return loopComplete;
	}
}
