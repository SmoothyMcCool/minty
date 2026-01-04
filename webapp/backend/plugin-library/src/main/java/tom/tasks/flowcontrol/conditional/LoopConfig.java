package tom.tasks.flowcontrol.conditional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class LoopConfig implements TaskConfigSpec {

	public static final String BranchExpression = "Branch Expression";

	private String expression;

	public LoopConfig() {
		expression = "";
	}

	public LoopConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(BranchExpression)) {
			expression = config.get(BranchExpression).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(BranchExpression, TaskConfigTypes.String);
		return config;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	public String getBranchExpression() {
		return expression;
	}

	@Override
	public Map<String, Object> getValues() {
		return Map.of(BranchExpression, expression);
	}
}
