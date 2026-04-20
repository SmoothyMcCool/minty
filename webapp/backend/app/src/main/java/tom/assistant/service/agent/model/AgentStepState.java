package tom.assistant.service.agent.model;

import tom.assistant.service.agent.llm.LlmResponse;
import tom.assistant.service.agent.llm.LlmStatus;

public class AgentStepState {

	private LlmStatus status = LlmStatus.PENDING;
	private LlmResponse response;

	public AgentStepState() {
	}

	public AgentStepState(LlmStatus status) {
		this.status = status;
	}

	public LlmStatus getStatus() {
		return status;
	}

	public void setStatus(LlmStatus status) {
		this.status = status;
	}

	public LlmResponse getResponse() {
		return response;
	}

	public void setResponse(LlmResponse response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "AgentStepState [status=" + status + ", response=" + response + "]";
	}

}
