package tom.api.model.project;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NodeType {
	Folder("Folder"), File("File");

	private final String value;

	NodeType(String value) {
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
