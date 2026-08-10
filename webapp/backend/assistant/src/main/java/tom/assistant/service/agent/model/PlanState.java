package tom.assistant.service.agent.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tom.Pair;
import tom.api.MintyObjectMapper;
import tom.assistant.service.agent.llm.LlmResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class PlanState {

	private static ObjectMapper Mapper = MintyObjectMapper.StandardJsonMapper;

	@JsonProperty("currentStep")
	private int currentStep;
	@JsonProperty("steps")
	private List<Pair<AgentStep, AgentStepState>> steps;

	@JsonProperty("statusMessages")
	private List<String> statusMessages;

	@JsonIgnore
	private boolean errored;

	public PlanState() {
		currentStep = 0;
		steps = new ArrayList<>();
		statusMessages = new ArrayList<>();
		errored = false;
	}

	public PlanState(List<AgentStep> steps) {
		this();
		for (AgentStep step : steps) {
			this.steps.add(new Pair<>(step, new AgentStepState()));
		}
	}

	public void start() {
		currentStep = 0;
	}

	public boolean advanceStep() {
		currentStep++;
		return isDone();
	}

	public Pair<AgentStep, AgentStepState> currentStep() {
		return steps.get(currentStep);
	}

	@JsonIgnore
	public int getCurrentStepIndex() {
		return currentStep;
	}

	public void addReplanStep() {
		AgentStep currentStep = currentStep().left();
		AgentStep replanStep = new AgentStep();
		replanStep.setId(currentStep.getId() + "a");
		replanStep.setName("Replan");
		replanStep.setType(AgentStepType.PLAN);
		replanStep.setVisibility(AgentResponseVisibility.INTERNAL);
		replanStep.setWorker("planner");
		steps.add(this.currentStep + 1, new Pair<>(replanStep, new AgentStepState()));
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

	public List<Pair<AgentStep, AgentStepState>> getSteps() {
		return steps;
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
		} catch (JacksonException e) {
			throw new RuntimeException("Failed to stringify PlanState object.", e);
		}
	}

	public void replaceRemaining(List<AgentStep> newSteps) {
		List<Pair<AgentStep, AgentStepState>> prefix = new ArrayList<>(steps.subList(0, currentStep + 1));
		prefix.addAll(newSteps.stream().map(step -> new Pair<AgentStep, AgentStepState>(step, new AgentStepState()))
				.toList());

		this.steps = prefix;
	}

	public List<Pair<AgentStep, AgentStepState>> toStepStateList() {
		return Collections.unmodifiableList(steps.subList(0, currentStep + 1));
	}

	public List<String> getStatusMessages() {
		return statusMessages;
	}

	public void setStatusMessages(List<String> statusMessages) {
		this.statusMessages = statusMessages;
	}

	public void addStatusMessage(String statusMessage) {
		this.statusMessages.add(statusMessage);
	}

	@JsonIgnore
	public boolean isErrored() {
		return errored;
	}

	@JsonIgnore
	public void setErrored(boolean errored) {
		this.errored = errored;
	}

}
