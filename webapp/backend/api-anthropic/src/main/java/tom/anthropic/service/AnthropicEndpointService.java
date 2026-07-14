package tom.anthropic.service;

import java.util.List;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.EndpointConfig;
import tom.llm.KeyRequiredException;
import tom.llm.service.LlmEndpointService;
import tom.user.model.User;

public class AnthropicEndpointService implements LlmEndpointService {

	private final EndpointConfig endpointConfig;

	public AnthropicEndpointService(EndpointConfig endpointConfig) {
		this.endpointConfig = endpointConfig;
	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {

		String apiKeyName = endpointConfig.apiKeyName();
		String apiKey = user.getDefaults().get(apiKeyName);
		if (apiKey == null) {
			if (endpointConfig.requiresKey()) {
				throw new KeyRequiredException(apiKeyName);
			} else {
				apiKey = "doesn't matter";
			}
		}

		AnthropicChatOptions.Builder chatOptionsBuilder = AnthropicChatOptions.builder()
				.baseUrl(endpointConfig.url().toString()).apiKey(apiKey).model(assistant.model()).topK(assistant.topK())
				.maxTokens(contextSize);
		if (assistant.temperature() != null) {
			chatOptionsBuilder.temperature(assistant.temperature());
		}

		AnthropicChatModel chatModel = AnthropicChatModel.builder().options(chatOptionsBuilder.build()).build();

		return ChatClient.builder(chatModel).defaultAdvisors(advisors).build();
	}

	@Override
	public ChatModel buildSimpleModel(User user, String modelName) {
		if (user == null) {
			return null;
		}

		String apiKey = user.getDefaults().get("Anthropic API Key");
		AnthropicChatOptions chatOptions = AnthropicChatOptions.builder().apiKey(apiKey).model(modelName).build();
		AnthropicChatModel chatModel = AnthropicChatModel.builder().options(chatOptions).build();
		return chatModel;
	}

	@Override
	public EmbeddingModel buildEmbeddingModel(User user, String embeddingModelName) {
		throw new NotSupportedException("Anthropic does not support embeddings.");
	}

}