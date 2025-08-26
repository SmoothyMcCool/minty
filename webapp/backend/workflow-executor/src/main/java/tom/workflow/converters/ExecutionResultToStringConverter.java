package tom.workflow.converters;

import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tom.model.ChatMessage;
import tom.workflow.tracking.model.ExecutionResult;

@Converter
public class ExecutionResultToStringConverter implements AttributeConverter<ExecutionResult, String> {

	private final Logger logger = LogManager.getLogger(ExecutionResultToStringConverter.class);

	public ExecutionResultToStringConverter() {
	}

	@Override
	public String convertToDatabaseColumn(ExecutionResult attribute) {
		JsonFactory factory = new JsonFactory();
		StringWriter writer = new StringWriter();

		try (JsonGenerator generator = factory.createGenerator(writer)) {
			generator.writeStartObject();

			generator.writeNumberField("startTime", attribute.getStartTime().toEpochMilli());
			generator.writeNumberField("endTime", attribute.getEndTime().toEpochMilli());

			generator.writeFieldName("results");
			generator.writeStartArray();
			for (List<Map<String, Object>> stepResults : attribute.getResults()) {
				generator.writeStartArray();
				for (Map<String, Object> taskResult : stepResults) {
					generator.writeStartObject();
					for (Map.Entry<String, Object> entry : taskResult.entrySet()) {
						generator.writeObjectField(entry.getKey(), entry.getValue());
					}
					generator.writeEndObject();
				}
				generator.writeEndArray();
			}
			generator.writeEndArray();

			generator.writeFieldName("errors");
			generator.writeStartArray();
			for (List<String> stepErrors : attribute.getErrors()) {
				generator.writeArray(stepErrors.toArray(new String[0]), 0, stepErrors.size());
			}
			generator.writeEndArray();

			generator.writeFieldName("chatMessages");
			generator.writeStartArray();
			for (ChatMessage chatMessage : attribute.getChatMessages()) {
				generator.writeStartObject();
				generator.writeBooleanField("user", chatMessage.isUser());
				generator.writeStringField("message", chatMessage.getMessage());
				generator.writeEndObject();
			}
			generator.writeEndArray();

			generator.writeEndObject();

		} catch (Exception e) {
			logger.warn("Caught exception while serializing ExecutionResult: ", e);
		}

		return writer.toString();
	}

	@Override
	public ExecutionResult convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}

		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());

			JsonNode root = mapper.readTree(dbData);

			ExecutionResult result = new ExecutionResult();
			result.setStartTime(Instant.ofEpochMilli(root.get("startTime").asLong()));
			result.setEndTime(Instant.ofEpochMilli(root.get("endTime").asLong()));

			// results
			List<List<Map<String, Object>>> results = new ArrayList<>();
			for (JsonNode stepArray : root.get("results")) {
				List<Map<String, Object>> stepResults = new ArrayList<>();
				for (JsonNode objNode : stepArray) {
					Map<String, Object> map = mapper.convertValue(objNode, new TypeReference<Map<String, Object>>() {
					});
					stepResults.add(map);
				}
				results.add(stepResults);
			}
			result.setResults(results);

			// errors
			List<List<String>> errors = new ArrayList<>();
			for (JsonNode errArray : root.get("errors")) {
				List<String> stepErrors = mapper.convertValue(errArray, new TypeReference<List<String>>() {
				});
				errors.add(stepErrors);
			}
			result.setErrors(errors);

			// chatMessages
			List<ChatMessage> chatMessages = new ArrayList<>();
			for (JsonNode msgNode : root.get("chatMessages")) {
				ChatMessage msg = mapper.convertValue(msgNode, ChatMessage.class);
				chatMessages.add(msg);
			}
			result.setChatMessages(chatMessages);

			return result;

		} catch (Exception e) {
			logger.warn("Could not deserialize ExecutionResult", e);
			return null;
		}
	}
}
