package tom.api.services.assistant;

import java.util.List;
import java.util.UUID;

import tom.model.Assistant;

public interface AssistantManagementService {

	final UUID DefaultAssistantId = UUID.fromString("00000000-0000-0000-0000-000000000000");
	final UUID ConversationNamingAssistantId = UUID.fromString("00000000-0000-0000-0000-000000000001");

	Assistant createAssistant(UUID userId, Assistant assistant);

	List<Assistant> listAssistants(UUID userId);

	Assistant findAssistant(UUID userId, UUID assistantId);

	boolean deleteAssistant(UUID id, UUID assistantId);

	Assistant updateAssistant(UUID userId, Assistant assistant);

	String getModelForAssistant(UUID userId, UUID assistantId);

}
