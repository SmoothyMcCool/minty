package tom.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.persistence.AttributeConverter;

public class ClassConverter<T> implements AttributeConverter<T, String> {

	private final Logger logger = LogManager.getLogger(ClassConverter.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final TypeReference<T> typeReference;

	public ClassConverter(TypeReference<T> typeReference) {
		this.typeReference = typeReference;
		objectMapper.registerModule(new JavaTimeModule());
	}

	@Override
	public String convertToDatabaseColumn(T attribute) {
		if (attribute == null) {
			return null;
		}

		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
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
		} catch (JsonProcessingException e) {
			logger.warn("Could not convert String to " + typeReference.getType() + ".", e);
			return null;
		}
	}

}
