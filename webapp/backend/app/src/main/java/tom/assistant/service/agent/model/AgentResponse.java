package tom.assistant.service.agent.model;

public class AgentResponse<T> {

	private AgentAction action;
	private T payload;
	private String reason;

	public AgentAction getAction() {
		return action;
	}

	public void setAction(AgentAction action) {
		this.action = action;
	}

	public T getPayload() {
		return payload;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
