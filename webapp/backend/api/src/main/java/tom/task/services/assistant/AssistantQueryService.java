package tom.task.services.assistant;

import java.util.stream.Stream;

import tom.model.Assistant;
import tom.model.AssistantQuery;

public interface AssistantQueryService {

	String ask(int userId, AssistantQuery query);

	String ask(Assistant assistant, String query, String conversationId);

	Stream<String> askStreaming(int userId, AssistantQuery query);

}
