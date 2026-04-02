package tom.tasks.transform.pipeline.operations;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import tom.api.task.Packet;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

@RunnableTask
public class ExpressionOperation implements TransformOperation {

	public static final String OperationName = "SpEL Expression";

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		String expression = config.getString()
				.orElseThrow(() -> new IllegalArgumentException("ExpressionOperation requires string config"));

		// Turn data.foo -> data[0].foo, same for text
		expression = expression.replaceAll("\\bdata\\.(\\w+)", "data[0].$1");
		expression = expression.replaceAll("\\btext\\.(\\w+)", "text[0].$1");

		try {
			// Evaluate with SpEL
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext context = new StandardEvaluationContext(packet);
			context.setBeanResolver(null);
			context.setPropertyAccessors(List.of(new SafePropertyAccessor()));

			try {
				// If the expression contains an assignment, SpEL will execute it.
				// If it doesn't, we simply evaluate it to a value.
				parser.parseExpression(expression).getValue(context);
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to apply SpEL expression: " + context, e);
			}

		} catch (Exception e) {
			String error = "ExpressionTransform caught exception while trying to apply SpEL expression: " + expression
					+ ", to input: " + packet.toString();
			throw new RuntimeException(error, e);
		}
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

	@Override
	public String getName() {
		return OperationName;
	}
}
