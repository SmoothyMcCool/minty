package tom.api.services.assistant;

import org.apache.commons.lang3.StringUtils;

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
		return StringUtils.isNotBlank(value);
	}

}
