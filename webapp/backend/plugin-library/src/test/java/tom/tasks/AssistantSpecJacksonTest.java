package tom.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tom.api.AssistantId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantSpec;

class AssistantSpecJacksonTest {

	@Test
	void shouldConvertAssistantIdToAssistantSpec() {
		// Arrange
		AssistantId id = new AssistantId(UUID.randomUUID());

		// Act
		AssistantSpec result = TaskUtils.safeConvert(id, AssistantSpec.class);

		// Assert
		assertNotNull(result, "Result should not be null");
		assertEquals(id, result.getAssistantId(), "AssistantId should be preserved");
		assertNull(result.getAssistant(), "Assistant should be null when created from ID");
	}

	@Test
	void shouldConvertAssistantToAssistantSpec() {
		// Arrange
		AssistantId id = new AssistantId(UUID.randomUUID());

		Assistant assistant = new Assistant(id, "Test Assistant", "gpt-5", 8192, 0.7, 5, "Test prompt", List.of(), // documentIds
				List.of("tool1"), // tools
				true, false);

		// Act
		AssistantSpec result = TaskUtils.safeConvert(assistant, AssistantSpec.class);

		// Assert
		assertNotNull(result, "Result should not be null");
		assertNotNull(result.getAssistant(), "Assistant should be populated");
		assertEquals(assistant, result.getAssistant(), "Assistant should match input");

		// Depending on your constructor logic:
		assertNull(result.getAssistantId(), "AssistantId should be null when created from Assistant");
	}

	@Test
	void shouldConvertMapToAssistantSpec() {
		// Arrange
		UUID uuid = UUID.randomUUID();

		Map<String, Object> input = new HashMap<>();
		input.put("assistantId", uuid.toString());
		input.put("assistant", null);

		// Act
		AssistantSpec result = TaskUtils.safeConvert(input, AssistantSpec.class);

		// Assert
		assertNotNull(result, "Result should not be null");

		assertNotNull(result.getAssistantId(), "AssistantId should be populated");
		assertEquals(uuid, result.getAssistantId().value(), "UUID should match");

		assertNull(result.getAssistant(), "Assistant should be null");
	}
}
