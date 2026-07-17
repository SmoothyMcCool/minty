package tom.openai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.EndpointConfig;
import tom.llm.KeyRequiredException;
import tom.llm.service.LlmEndpointService;
import tom.tool.auditing.AuditingToolCallingManager;
import tom.user.model.User;

public class OpenAiEndpointService implements LlmEndpointService {

	private final ToolCallingManager defaultToolCallingManager;
	private final EndpointConfig endpointConfig;

	public OpenAiEndpointService(EndpointConfig endpointConfig) {
		defaultToolCallingManager = DefaultToolCallingManager.builder().build();
		this.endpointConfig = endpointConfig;
	}

	@Override
	public EmbeddingModel buildEmbeddingModel(User user, String embeddingModelName) {
		OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder().model(embeddingModelName)
				.apiKey(getApiKey(user)).baseUrl(endpointConfig.url().toString()).timeout(endpointConfig.apiTimeout())
				.build();
		return OpenAiEmbeddingModel.builder().metadataMode(MetadataMode.EMBED).options(embeddingOptions).build();
	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {
		OpenAiChatOptions.Builder chatOptionsBuilder = OpenAiChatOptions.builder().model(assistant.model())
				.apiKey(getApiKey(user)).baseUrl(endpointConfig.url().toString()).timeout(endpointConfig.apiTimeout());
		if (assistant.temperature() != null) {
			chatOptionsBuilder.temperature(assistant.temperature());
		}
		if (assistant.topK() != null) {
			chatOptionsBuilder.topK(assistant.topK());
		}

		ToolCallingManager auditingManager = new AuditingToolCallingManager(
				query.getConversationId().getValue().toString(), defaultToolCallingManager);
		List<Advisor> allAdvisors = new ArrayList<>();
		allAdvisors.add(ToolCallingAdvisor.builder().toolCallingManager(auditingManager).build());
		allAdvisors.addAll(advisors);

		ChatModel chatModel = OpenAiChatModel.builder().options(chatOptionsBuilder.build()).build();
		return ChatClient.builder(chatModel).defaultAdvisors(allAdvisors).build();
	}

	@Override
	public ChatModel buildSimpleModel(User user, String modelName) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().model(modelName).apiKey(getApiKey(user))
				.baseUrl(endpointConfig.url().toString()).timeout(endpointConfig.apiTimeout()).build();
		return OpenAiChatModel.builder().options(options).build();
	}

	private String getApiKey(User user) {
		String apiKeyName = endpointConfig.apiKeyName();
		String apiKey = "not defined";
		if (user != null && user.getDefaults() != null) {
			apiKey = user.getDefaults().get(apiKeyName);
			if (apiKey == null) {
				if (endpointConfig.requiresKey()) {
					throw new KeyRequiredException(apiKeyName);
				} else {
					apiKey = "doesn't matter";
				}
			}
		}
		return apiKey;
	}
}