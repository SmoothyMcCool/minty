package tom.assistant.service.agent.model;

import java.util.List;

import tom.assistant.service.agent.response.LlmResponse;

public class AgentState {

	private String userQuery;
	private String currentStep;
	private List<AgentStepState> plan;
	private List<LlmResponse> responses;

	public String getUserQuery() {
		return userQuery;
	}

	public void setUserQuery(String userQuery) {
		this.userQuery = userQuery;
	}

	public String getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(String currentStep) {
		this.currentStep = currentStep;
	}

	public List<AgentStepState> getPlan() {
		return plan;
	}

	public void setPlan(List<AgentStepState> plan) {
		this.plan = plan;
	}

	public List<LlmResponse> getResponses() {
		return responses;
	}

	public void setResponses(List<LlmResponse> responses) {
		this.responses = responses;
	}

}
