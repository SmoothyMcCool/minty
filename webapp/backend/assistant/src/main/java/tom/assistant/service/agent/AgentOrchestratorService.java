package tom.assistant.service.agent;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;

public interface AgentOrchestratorService {

	void setAssistantQueryService(AssistantQueryService assistantQueryService);

	void execute(UserId userId, AssistantQuery query, StreamResult sr);
}
