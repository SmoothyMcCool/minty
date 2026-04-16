package tom.assistant.service.agent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.Pair;
import tom.assistant.service.agent.response.LlmResponse;

public class PlanState {

	private static ObjectMapper Mapper = new ObjectMapper();

	@JsonProperty("currentStepId")
	private String currentStepId;
	private int currentStep;
	@JsonProperty("steps")
	private List<Pair<AgentStep, AgentStepState>> steps;

	public PlanState(List<AgentStep> steps) {
		this.currentStep = 0;
		this.currentStepId = steps.get(currentStep).getId();
		this.steps = new ArrayList<>();
		for (AgentStep step : steps) {
			this.steps.add(new Pair<>(step, new AgentStepState()));
		}
	}

	public void start() {
		currentStepId = steps.get(0).left().getId();
	}

	public boolean advanceStep() {
		currentStep++;
		if (currentStep < steps.size()) {
			currentStepId = steps.get(currentStep).left().getId();
		}
		return isDone();
	}

	public Pair<AgentStep, AgentStepState> currentStep() {
		return steps.get(currentStep);
	}

	public Optional<Pair<AgentStep, AgentStepState>> findLastCompletedStep() {
		if (currentStep > 0) {
			return Optional.of(steps.get(currentStep - 1));
		}
		return Optional.empty();
	}

	public void setResult(JsonNode input) {
		LlmResponse result = new LlmResponse();
		result.setData(input);
		steps.get(currentStep).right().setResponse(result);
	}

	@JsonIgnore
	public boolean isDone() {
		return currentStep == steps.size();
	}

	@JsonIgnore
	public boolean isAtStart() {
		return currentStep == 0;
	}

	@Override
	public String toString() {
		try {
			return Mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to stringify PlanState object.", e);
		}
	}

}
