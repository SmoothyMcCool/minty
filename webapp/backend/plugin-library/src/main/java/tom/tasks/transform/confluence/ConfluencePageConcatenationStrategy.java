package tom.tasks.transform.confluence;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfluencePageConcatenationStrategy {
	MultiPacket("Multiple Packets"), Array("Array"), Concatenated("Concatenated");

	private final String value;

	ConfluencePageConcatenationStrategy(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}
}
