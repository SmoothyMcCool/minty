package tom.assistant.service.agent.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AgentAction {
	ASK_USER("ASK_USER"), HANDOFF("HANDOFF"), RESULT("RESULT"), VALID("VALID"), INVALID("INVALID"), DONE("DONE"),
	CANCEL("CANCEL");

	private final String value;

	AgentAction(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
