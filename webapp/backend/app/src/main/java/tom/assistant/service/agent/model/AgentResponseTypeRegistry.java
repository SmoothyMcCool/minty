package tom.assistant.service.agent.model;

import java.util.HashMap;
import java.util.Map;

public class AgentResponseTypeRegistry {

	private final Map<AgentAction, Class<?>> registry = new HashMap<>();

	public void register(AgentAction action, Class<?> payloadClass) {
		registry.put(action, payloadClass);
	}

	public Class<?> get(AgentAction action) {
		return registry.getOrDefault(action, Object.class);
	}
}
