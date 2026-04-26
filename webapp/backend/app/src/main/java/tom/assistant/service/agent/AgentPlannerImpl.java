package tom.assistant.service.agent;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.PlanState;

@Service
public class AgentPlannerImpl implements AgentPlanner {

	private static final Logger logger = LogManager.getLogger(AgentPlannerImpl.class);

	private AssistantQueryService assistantQueryService;
	private final AgentRegistryImpl agentRegistry;

	private static final ObjectMapper Mapper = new ObjectMapper();

	public AgentPlannerImpl(AgentRegistryImpl agentRegistry) {

		this.agentRegistry = agentRegistry;
	}

	@Override
	public void setAssistantQueryService(AssistantQueryService assistantQueryService) {
		this.assistantQueryService = assistantQueryService;
	}

	@Override
	public List<AgentStep> plan(UserId userId, AssistantQuery query, PlanState state) {
		int retryCount = 0;
		String json = "";
		while (retryCount < 3) {
			AgentQuery plannerQuery = buildPlannerQuery(query, state);
			json = assistantQueryService.runSingleLlmCall(userId, plannerQuery.query());
			try {
				return parse(json);
			} catch (Exception e) {
				logger.warn("Planner generated invalid plan. Retrying.");
				retryCount++;
			}
		}
		throw new RuntimeException("Planner repeatedly failed to produce a plan. Produced " + json);
	}

	private AgentQuery buildPlannerQuery(AssistantQuery original, PlanState state) {
		return agentRegistry.getPlanner("planner", original, state);
	}

	private static List<AgentStep> parse(String json) {
		try {
			JsonNode node = Mapper.readTree(json);
			if (node.isArray()) {
				return Mapper.convertValue(node, new TypeReference<List<AgentStep>>() {
				});
			} else {
				return List.of(Mapper.convertValue(node, new TypeReference<AgentStep>() {
				}));
			}

		} catch (

		Exception e) {
			throw new RuntimeException("Failed to parse planner output: " + json, e);
		}
	}

}
