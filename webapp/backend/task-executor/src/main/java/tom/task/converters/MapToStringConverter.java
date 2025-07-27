package tom.task.converters;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, String>, String> {

	private final Logger logger = LogManager.getLogger(MapToStringConverter.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, String> attribute) {
		if (attribute == null) {
			return null;
		}

		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			logger.warn("Could not convert Map to String.");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}

		try {
			return objectMapper.readValue(dbData, Map.class);
		} catch (JsonProcessingException e) {
			logger.warn("Could not convert String to Map.");
			return null;
		}
	}

}
