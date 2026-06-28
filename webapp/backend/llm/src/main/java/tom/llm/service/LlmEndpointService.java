package tom.llm.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.user.model.User;

public interface LlmEndpointService {

	ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors);

	EmbeddingModel buildEmbeddingModel(User user, String embeddingModelName);

	ChatModel buildSimpleModel(User user, String modelName);

}
