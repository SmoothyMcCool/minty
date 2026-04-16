package tom.assistant.service.agent;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentStep;

@Service
public class AgentPlannerImpl implements AgentPlanner {

	private static final Logger logger = LogManager.getLogger(AgentPlannerImpl.class);

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
		int retryCount = 0;
		while (retryCount < 3) {
			AgentQuery plannerQuery = buildPlannerQuery(query);
			String json = assistantQueryService.runSingleLlmCall(userId, plannerQuery.query());
			try {
				return parse(json);
			} catch (Exception e) {
				logger.warn("Planner generated invalid plan. Retrying.");
			} finally {
				retryCount++;
			}
		}
		throw new RuntimeException("Planner repeatedly failed to produce a plan.");
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
