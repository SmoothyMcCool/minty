package tom.workflow.converters;

import java.io.IOException;
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

	private static final Logger logger = LogManager.getLogger(ExecutionResultToStringConverter.class);

	private final ObjectMapper mapper;

	public ExecutionResultToStringConverter() {
		mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
	}

	@Override
	public String convertToDatabaseColumn(ExecutionResult attribute) {
		if (attribute == null) {
			return null;
		}

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
						writeJsonValue(generator, entry.getKey(), entry.getValue());
					}
					generator.writeEndObject();
				}
				generator.writeEndArray();
			}
			generator.writeEndArray();

			generator.writeFieldName("errors");
			generator.writeStartArray();
			for (List<String> stepErrors : attribute.getErrors()) {
				writeJsonValue(generator, null, stepErrors);
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
			return null;
		}

		return writer.toString();
	}

	private static void writeJsonValue(JsonGenerator generator, String fieldName, Object value) throws IOException {
		if (value == null) {
			return;
		}

		if (fieldName != null) {
			// We are writing an object.

			if (value instanceof List<?> || (value != null && value.getClass().isArray())) {
				generator.writeArrayFieldStart(fieldName);
				writeIterable(generator, value);
				generator.writeEndArray();
			} else if (value instanceof Map<?, ?>) {
				generator.writeObjectFieldStart(fieldName);
				writeMap(generator, (Map<?, ?>) value);
				generator.writeEndObject();
			} else {
				generator.writeObjectField(fieldName, value);
			}
		}

		else {
			// Writing inside an array - no field name
			if (value instanceof List<?> || (value != null && value.getClass().isArray())) {
				generator.writeStartArray();
				writeIterable(generator, value);
				generator.writeEndArray();
			} else if (value instanceof Map<?, ?>) {
				generator.writeStartObject();
				writeMap(generator, (Map<?, ?>) value);
				generator.writeEndObject();
			} else {
				generator.writeObject(value);
			}
		}
	}

	private static void writeIterable(JsonGenerator generator, Object iterableOrArray) throws IOException {
		if (iterableOrArray instanceof List<?>) {
			for (Object item : (List<?>) iterableOrArray) {
				writeJsonValue(generator, null, item);
			}
		} else if (iterableOrArray != null && iterableOrArray.getClass().isArray()) {
			int length = java.lang.reflect.Array.getLength(iterableOrArray);
			for (int i = 0; i < length; i++) {
				writeJsonValue(generator, null, java.lang.reflect.Array.get(iterableOrArray, i));
			}
		}
	}

	private static void writeMap(JsonGenerator generator, Map<?, ?> map) throws IOException {
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			writeJsonValue(generator, String.valueOf(entry.getKey()), entry.getValue());
		}
	}

	@Override
	public ExecutionResult convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return null;
		}

		try {

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

		} catch (

		Exception e) {
			logger.warn("Could not deserialize ExecutionResult", e);
			return null;
		}
	}
}
