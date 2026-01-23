package tom.tasks.transform.simple;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
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
public class SimpleTransform implements MintyTask {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private SimpleTransformConfig config;
	private Packet input;
	private Packet result;
	private String error;
	private boolean failed;

	public SimpleTransform() {
		outputs = List.of();
		config = null;
		result = new Packet();
		error = null;
		input = null;
		failed = false;
	}

	public SimpleTransform(SimpleTransformConfig config) {
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
		String expression = config.getExpression();

		// Turn data.foo -> data[0].foo, same for text
		expression = expression.replaceAll("\\bdata\\.(\\w+)", "data[0].$1");
		expression = expression.replaceAll("\\btext\\.(\\w+)", "text[0].$1");

		try {
			// Evaluate with SpEL
			result = input;
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext context = new StandardEvaluationContext(result);
			context.setBeanResolver(null);
			context.setPropertyAccessors(List.of(new SafePropertyAccessor()));

			try {
				// If the expression contains an assignment, SpEL will execute it.
				// If it doesn't, we simply evaluate it to a value.
				String preTransformPacket = input.toJson();
				parser.parseExpression(expression).getValue(context);
				String postTransformPacket = result.toJson();
				logger.debug("Transformed " + preTransformPacket + " to " + postTransformPacket);
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to apply SpEL expression: " + context, e);
			}

			outputs.get(0).write(result);
		} catch (Exception e) {
			String error = "SimpleTransform caught exception while trying to apply SpEL expression: " + expression
					+ ", to input: " + input.toString();
			logger.error(error);
			throw new RuntimeException(error, e);
		}
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"Workflow misconfiguration detect. SimpleTransform should only ever have exactly one input!");
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
				return "This task accepts any input. It will perform a simply transformation based on the given SpEL expression.";
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
				return new SimpleTransformConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new SimpleTransformConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Simple Transform";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
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

	private static class SafePropertyAccessor implements org.springframework.expression.PropertyAccessor {
		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] { Packet.class, Map.class };
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			try {
				BeanWrapper bw = new BeanWrapperImpl(target);
				Object value = bw.getPropertyValue(name);
				return new TypedValue(value);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot read property " + name, e);
			}
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			try {
				BeanWrapper bw = new BeanWrapperImpl(target);
				bw.setPropertyValue(name, newValue);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot write property " + name, e);
			}
		}
	}
}
