package tom.assistant.service.agent;

import tom.api.model.assistant.AssistantQuery;

public record AgentQuery(AgentResponseType responsetype, AssistantQuery query) {

}
