package tom.api.model.project;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FileType {
	code("code"), markdown("markdown"), json("json"), yaml("yaml"), text("text"), diagram("diagram"), html("html");

	private final String value;

	FileType(String value) {
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
