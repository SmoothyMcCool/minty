package tom.api.services.assistant;

public final class StringResult implements LlmResult {

	private String value = "";

	public StringResult() {
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public boolean isComplete() {
		return !value.isBlank();
	}

}
