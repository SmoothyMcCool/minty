package tom.assistant.service.agent.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.Pair;
import tom.api.model.assistant.AssistantQuery;

public class AgentInput {

	private String query;
	private List<Pair<AgentStep, AgentStepState>> state;
	private String reason;
	private String format;
	private Map<String, Object> schema;
	private String rules;

	private AgentInput() {
	}

	@SuppressWarnings("unchecked")
	public static AgentInput resolve(AssistantQuery userQuery, PlanState planState) {
		AgentInput input = new AgentInput();

		// Start with the user's original query
		input.query = userQuery.getQuery();

		// Override with step-level query if the planner provided one
		Map<String, Object> stepInput = Map.of();
		if (planState != null) {
			Pair<AgentStep, AgentStepState> currentStep = planState.currentStep();
			stepInput = currentStep.left().getInput();
		}

		if (stepInput.containsKey("query")) {
			input.query = (String) stepInput.get("query");
		}
		if (stepInput.containsKey("reason")) {
			input.reason = (String) stepInput.get("reason");
		}
		if (stepInput.containsKey("format")) {
			input.format = (String) stepInput.get("format");
		}
		if (stepInput.containsKey("schema")) {
			input.schema = (Map<String, Object>) stepInput.get("schema");
		}
		if (stepInput.containsKey("rules")) {
			input.rules = (String) stepInput.get("rules");
		}

		if (planState != null) {
			input.state = planState.toStepStateList();
		}

		return input;
	}

	public String serialize() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize AgentInput", e);
		}
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public List<Pair<AgentStep, AgentStepState>> getState() {
		return state;
	}

	public void setState(List<Pair<AgentStep, AgentStepState>> state) {
		this.state = state;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Map<String, Object> getSchema() {
		return schema;
	}

	public void setSchema(Map<String, Object> schema) {
		this.schema = schema;
	}

	public String getRules() {
		return rules;
	}

	public void setRules(String rules) {
		this.rules = rules;
	}

	@Override
	public String toString() {
		return "AgentInput [query=" + query + ", state=" + state + ", reason=" + reason + ", format=" + format
				+ ", schema=" + schema + ", rules=" + rules + "]";
	}

}