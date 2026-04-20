package tom.assistant.service.agent;

import java.util.List;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.PlanState;

public interface AgentPlanner {

	default List<AgentStep> plan(UserId userId, AssistantQuery query) {
		return plan(userId, query, null);
	}

	List<AgentStep> plan(UserId userId, AssistantQuery query, PlanState state);

	void setAssistantQueryService(AssistantQueryService assistantQueryService);

}
