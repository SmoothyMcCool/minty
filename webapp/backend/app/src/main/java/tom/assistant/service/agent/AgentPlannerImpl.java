package tom.assistant.service.agent;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;

@Service
public class AgentPlannerImpl implements AgentPlanner {

	private AssistantQueryService assistantQueryService;
	private final WorkerQueryFactoryService workerQueryFactoryService;

	private static final ObjectMapper Mapper = new ObjectMapper();

	public AgentPlannerImpl(WorkerQueryFactoryService workerQueryFactoryService) {

		this.workerQueryFactoryService = workerQueryFactoryService;
	}

	@Override
	public void setAssistantQueryService(AssistantQueryService assistantQueryService) {
		this.assistantQueryService = assistantQueryService;
	}

	@Override
	public List<AgentStep> plan(UserId userId, AssistantQuery query) {
		AgentQuery plannerQuery = buildPlannerQuery(query);

		String json = assistantQueryService.runSingleLlmCall(userId, plannerQuery.query());

		return parse(json);
	}

	private AgentQuery buildPlannerQuery(AssistantQuery original) {
		return workerQueryFactoryService.planner(original.getQuery(), original.getConversationId(),
				original.getContextSize());
	}

	private static List<AgentStep> parse(String json) {
		try {
			return Mapper.readValue(json, new TypeReference<List<AgentStep>>() {
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse planner output: " + json, e);
		}
	}

}
