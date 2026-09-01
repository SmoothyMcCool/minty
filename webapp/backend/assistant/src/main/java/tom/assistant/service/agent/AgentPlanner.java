package tom.assistant.service.agent;

import java.util.List;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.PlanState;

public interface AgentPlanner {

	default List<AgentStep> plan(UserId userId, AssistantQuery query, StreamResult sr) throws InterruptedException {
		return plan(userId, query, null, sr);
	}

	List<AgentStep> plan(UserId userId, AssistantQuery query, PlanState state, StreamResult sr)
			throws InterruptedException;

	void setAssistantQueryService(AssistantQueryService assistantQueryService);

}
