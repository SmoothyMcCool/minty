package tom.assistant.service.agent.worker;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NextAction {
	ASK_USER("ASK_USER"), LLM_CALL("LLM_CALL"), RESULT("RESULT"), DONE("DONE"), ERROR("ERROR");

	private final String value;

	NextAction(String value) {
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
