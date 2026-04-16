package tom.assistant.service.agent.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import tom.assistant.service.agent.response.LlmResponse;

public class WorkerDecision {

	private NextAction action;
	private JsonNode input;
	private String reason;

	private static final ObjectMapper mapper = new ObjectMapper();

	public NextAction getAction() {
		return action;
	}

	public void setAction(NextAction action) {
		this.action = action;
	}

	public JsonNode getInput() {
		return input;
	}

	public void setInput(JsonNode input) {
		this.input = input;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "action:" + action + ", input:" + input + ", reason:" + reason;
	}

	public static WorkerDecision llmCall(String type) {
		ObjectNode input = mapper.createObjectNode();
		input.put("type", type);

		return builder().action(NextAction.LLM_CALL).input(input).reason("LLM call: " + type).build();
	}

	public static WorkerDecision error(String message) {
		return builder().action(NextAction.ERROR).reason(message).build();
	}

	public static WorkerDecision needInfo(LlmResponse response) {
		return builder().action(NextAction.ASK_USER).input(response.getData()).reason("Asking user").build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private NextAction action;
		private JsonNode input;
		private String reason;

		private Builder() {
		}

		public Builder action(NextAction action) {
			this.action = action;
			return this;
		}

		public Builder input(JsonNode input) {
			this.input = input;
			return this;
		}

		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}

		public WorkerDecision build() {
			WorkerDecision decision = new WorkerDecision();
			decision.setAction(this.action);
			decision.setInput(this.input);
			decision.setReason(this.reason);
			return decision;
		}
	}

}