package tom.api.tool;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ToolStatus {
	Ok("ok"), Error("error");

	private final String value;

	ToolStatus(String value) {
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
