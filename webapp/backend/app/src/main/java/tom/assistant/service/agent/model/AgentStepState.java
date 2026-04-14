package tom.assistant.service.agent.model;

import tom.assistant.service.agent.response.LlmStatus;

public class AgentStepState {

	private String step = "";
	private LlmStatus status = LlmStatus.UNKNOWN;

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}

	public LlmStatus getStatus() {
		return status;
	}

	public void setStatus(LlmStatus status) {
		this.status = status;
	}

}
