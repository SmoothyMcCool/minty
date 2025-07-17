package tom.task.services;

import java.util.List;

import tom.task.model.Assistant;
import tom.task.model.AssistantQuery;

public interface AssistantService {

    Assistant createAssistant(int userId, Assistant assistant);

    List<Assistant> listAssistants(int userId);

    Assistant findAssistant(int userId, int assistantId);

    boolean deleteAssistant(int id, int assistantId);

    String ask(int id, AssistantQuery query);

}
