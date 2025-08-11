package tom.task.services.assistant;

import java.util.List;

import tom.model.Assistant;

public interface AssistantManagementService {

	int DefaultAssistantId = 0;

	Assistant createAssistant(int userId, Assistant assistant);

	List<Assistant> listAssistants(int userId);

	Assistant findAssistant(int userId, int assistantId);

	boolean deleteAssistant(int id, int assistantId);

	Assistant updateAssistant(int userId, Assistant assistant);

	String getModelForAssistant(int userId, int assistantId);

}
