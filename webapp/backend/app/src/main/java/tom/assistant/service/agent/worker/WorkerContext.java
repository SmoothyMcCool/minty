package tom.assistant.service.agent.worker;

import java.util.Map;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.StreamResult;
import tom.assistant.service.agent.model.AgentStep;

public class WorkerContext {
	public final UserId userId;
	public final AssistantQuery query;
	public final AgentStep step;
	public final Map<String, Object> state;
	public final StreamResult stream;

	public WorkerContext(UserId userId, AssistantQuery query, AgentStep step, Map<String, Object> state,
			StreamResult stream) {
		this.userId = userId;
		this.query = query;
		this.step = step;
		this.state = state;
		this.stream = stream;
	}
}
