package tom.assistant.service.agent;

import java.util.List;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;

public interface AgentPlanner {

	List<AgentStep> plan(UserId userId, AssistantQuery query);

}
