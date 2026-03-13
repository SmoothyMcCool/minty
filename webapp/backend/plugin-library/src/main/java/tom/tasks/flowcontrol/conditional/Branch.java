package tom.tasks.flowcontrol.conditional;

import java.util.List;
import java.util.Map;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class Branch extends MintyTask {

	private List<? extends OutputPort> outputs;

	private Packet input;
	private ConditionalConfig config;
	private boolean conditionMet;

	public Branch() {
		input = null;
		config = null;
		conditionMet = false;
	}

	public Branch(ConditionalConfig config) {
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
		try {
			String expression = config.getBranchExpression();
			Packet result = input;
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setVariable("p", result);
			Object raw = parser.parseExpression(expression).getValue(context);

			if (!(raw instanceof Boolean)) {
				throw new IllegalArgumentException("Expression did not return a boolean value: " + raw);
			}

			conditionMet = (Boolean) raw;
			int outputPort = conditionMet ? 0 : 1;
			outputs.get(outputPort).write(result);

		} catch (Exception e) {
			String error = "Branch caught exception while applying SpEL: " + config.getBranchExpression()
					+ ", to input: " + input;
			error(error);
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
				return "Evaluate a condition. If true, emit packet on output 0. If false, output 1. Don't use this task if you want looping, use Loop.";
			}

			@Override
			public String expects() {
				return "Any packet, against which an expression is evaluated to determine the output port.";
			}

			@Override
			public String produces() {
				return "Each input is sent unmodified to output 0 if the condition evaluates to true, or 1 if the condition is false.";
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
				return "Branch";
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

}
