package tom.workflow.converters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tom.workflow.controller.WorkflowRequest;

@Converter
public class WorkflowRequestConverter implements AttributeConverter<WorkflowRequest, String> {

	private final Logger logger = LogManager.getLogger(WorkflowRequestConverter.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(WorkflowRequest attribute) {
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
	public WorkflowRequest convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}

		try {
			return objectMapper.readValue(dbData, WorkflowRequest.class);
		} catch (JsonProcessingException e) {
			logger.warn("Could not convert String to Map.");
			return null;
		}
	}

}
