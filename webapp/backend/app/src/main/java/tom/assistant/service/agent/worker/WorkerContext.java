package tom.assistant.service.agent.worker;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.StreamResult;
import tom.assistant.service.agent.model.PlanState;

public class WorkerContext {
	public final UserId userId;
	public final AssistantQuery query;
	public final PlanState state;
	public final StreamResult stream;

	public WorkerContext(UserId userId, AssistantQuery query, PlanState state, StreamResult stream) {
		this.userId = userId;
		this.query = query;
		this.state = state;
		this.stream = stream;
	}
}
