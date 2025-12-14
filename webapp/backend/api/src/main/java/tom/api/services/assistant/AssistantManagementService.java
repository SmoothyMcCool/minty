package tom.api.services.assistant;

import java.util.List;
import java.util.UUID;

import tom.api.AssistantId;
import tom.api.UserId;
import tom.api.model.Assistant;

public interface AssistantManagementService {

	final AssistantId DefaultAssistantId = new AssistantId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
	final AssistantId ConversationNamingAssistantId = new AssistantId(
			UUID.fromString("00000000-0000-0000-0000-000000000001"));
	final AssistantId DiagrammingAssistantId = new AssistantId(UUID.fromString("00000000-0000-0000-0000-000000000002"));

	Assistant createAssistant(UserId userId, Assistant assistant);

	List<Assistant> listAssistants(UserId userId);

	Assistant findAssistant(UserId userId, AssistantId assistantId);

	boolean deleteAssistant(UserId userId, AssistantId assistantId);

	Assistant updateAssistant(UserId userId, Assistant assistant);

	String getModelForAssistant(UserId userId, AssistantId assistantId);

	boolean isAssistantConversational(AssistantId assistantId);

}
