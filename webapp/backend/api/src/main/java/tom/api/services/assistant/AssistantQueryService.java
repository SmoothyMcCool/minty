package tom.api.services.assistant;

import java.util.UUID;
import java.util.stream.Stream;

import tom.model.Assistant;
import tom.model.AssistantQuery;

public interface AssistantQueryService {

	String ask(UUID userId, AssistantQuery query);

	String ask(Assistant assistant, String query, UUID conversationId);

	String ask(Assistant assistant, String query);

	Stream<String> askStreaming(UUID userId, AssistantQuery query);

}
