package tom.task.converters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tom.task.controller.TaskRequest;

@Converter
public class TaskRequestConverter implements AttributeConverter<TaskRequest, String> {

	private final Logger logger = LogManager.getLogger(TaskRequestConverter.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(TaskRequest attribute) {
		if (attribute == null) {
			return "";
		}

		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			logger.warn("Could not convert Map to String.");
			return "";
		}
	}

	@Override
	public TaskRequest convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}

		try {
			return objectMapper.readValue(dbData, TaskRequest.class);
		} catch (JsonProcessingException e) {
			logger.warn("Could not convert String to Map.");
			return null;
		}
	}

}
