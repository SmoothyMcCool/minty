package tom.assistant.service.agent;

import java.util.List;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;

public interface AgentPlanner {

	List<AgentStep> plan(UserId userId, AssistantQuery query);

	void setAssistantQueryService(AssistantQueryService assistantQueryService);

}
