package tom.task.services;

import java.util.List;
import java.util.stream.Stream;

import tom.model.Assistant;
import tom.model.AssistantQuery;

public interface AssistantService {

	Assistant createAssistant(int userId, Assistant assistant);

	List<Assistant> listAssistants(int userId);

	Assistant findAssistant(int userId, int assistantId);

	boolean deleteAssistant(int id, int assistantId);

	String ask(int userId, AssistantQuery query);

	Stream<String> askStreaming(int userId, AssistantQuery query);

	void fileCompleteFor(int assistantId);

	void setDocumentService(DocumentService documentService);

	String getModelForAssistant(int userId, int assistantId);

}
