package tom.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.persistence.AttributeConverter;
import tom.api.MintyObjectMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class ClassConverter<T> implements AttributeConverter<T, String> {

	private final Logger logger = LogManager.getLogger(ClassConverter.class);
	private final ObjectMapper objectMapper = MintyObjectMapper.StandardJsonMapper;
	private final TypeReference<T> typeReference;

	public ClassConverter(TypeReference<T> typeReference) {
		this.typeReference = typeReference;
	}

	@Override
	public String convertToDatabaseColumn(T attribute) {
		if (attribute == null) {
			return null;
		}

		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JacksonException e) {
			logger.warn("Could not convert " + typeReference.getType() + " to String.", e);
			return null;
		}
	}

	@Override
	public T convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}

		try {
			return objectMapper.readValue(dbData, typeReference);
		} catch (JacksonException e) {
			logger.warn("Could not convert String to " + typeReference.getType() + ".", e);
			return null;
		}
	}

}
