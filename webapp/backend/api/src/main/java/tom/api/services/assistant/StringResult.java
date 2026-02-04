package tom.api.services.assistant;

public final class StringResult implements LlmResult {

	private String value = "";
	private LlmResultState resultState = LlmResultState.QUEUED;

	public StringResult() {
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public boolean isComplete() {
		return resultState == LlmResultState.COMPLETE;
	}

	public void setState(LlmResultState resultState) {
		this.resultState = resultState;
	}

	public LlmResultState getState() {
		return resultState;
	}
}
