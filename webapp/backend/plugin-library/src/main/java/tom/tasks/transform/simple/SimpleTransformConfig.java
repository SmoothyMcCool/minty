package tom.tasks.transform.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class SimpleTransformConfig implements TaskConfigSpec {

	public static final String TransformExpression = "Expression";

	private String expression;

	public SimpleTransformConfig() {
		expression = "";
	}

	public SimpleTransformConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(TransformExpression)) {
			expression = config.get(TransformExpression).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(TransformExpression, TaskConfigTypes.String);
		return config;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	@Override
	public Map<String, Object> getValues() {
		return Map.of(TransformExpression, expression);
	}
}
