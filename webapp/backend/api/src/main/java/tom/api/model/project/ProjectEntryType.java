package tom.api.model.project;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectEntryType {
	Folder("folder"), RequirementsDoc("reqts"), DesignDoc("design"), Story("story"), File("file"), Unknown("unknown");

	private final String value;

	ProjectEntryType(String value) {
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
