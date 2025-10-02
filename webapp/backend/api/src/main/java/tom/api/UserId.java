package tom.api;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record UserId(UUID value) implements Serializable {

	@JsonCreator
	public UserId(String strValue) {
		this(strValue == null || strValue.isBlank() ? null : UUID.fromString(strValue));
	}

	@JsonValue
	public UUID getValue() {
		return value;
	}
}
