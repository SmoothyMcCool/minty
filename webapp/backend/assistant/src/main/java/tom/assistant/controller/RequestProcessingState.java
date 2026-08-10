package tom.assistant.controller;

public enum RequestProcessingState {
	NOT_READY("NOT_READY"), RUNNING("RUNNING"), COMPLETE("COMPLETE");

	private final String value;

	RequestProcessingState(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
