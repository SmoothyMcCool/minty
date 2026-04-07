package tom.assistant.service.agent;

import java.util.List;

import org.springframework.stereotype.Service;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;

@Service
public class AgentPlannerImpl implements AgentPlanner {

	private final AssistantQueryService assistantQueryService;

	public AgentPlannerImpl(AssistantQueryService assistantQueryService) {
		this.assistantQueryService = assistantQueryService;
	}

	public List<AgentStep> plan(UserId userId, AssistantQuery query) {
		return null;
		// AssistantQuery plannerQuery = PlannerPrompts.buildPlannerQuery(query);

		// String json = assistantQueryService.runSingleLlmCall(userId, plannerQuery);

		// return PlannerPrompts.parse(json);
	}
}
