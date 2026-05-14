package tom.meta.model;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record LlmRequestId(UUID value) {

	@JsonCreator
	public LlmRequestId(String strValue) {
		this(StringUtils.isBlank(strValue) ? null : UUID.fromString(strValue));
	}

	@JsonValue
	public UUID toJson() {
		return value;
	}

}
