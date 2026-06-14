package tom.anthropic.service;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.EndpointConfig;
import tom.llm.service.LlmEndpointService;
import tom.user.model.User;

public class AnthropicEndpointService implements LlmEndpointService {

	private final VectorStore vectorStore;
	private final ChatMemoryRepository chatMemoryRepository;
	private final ChatMemory chatMemory;
	private final EmbeddingModel embeddingModel;

	public AnthropicEndpointService(EndpointConfig endpointConfig, JdbcTemplate vectorJdbcTemplate,
			DataSource dataSource, String embeddingModelName, int chatMemoryDepth) {
		// Anthropic doesn't have an embedding API, so we have no embeddingModel
		// to build here. If you need embeddings for RAG on Anthropic-backed
		// assistants you'll need a separate embedding endpoint (e.g. a shared
		// Ollama or OpenAI one). For now vectorStore is left uninitialized and
		// getVectorStore() will throw to make that obvious.
		this.embeddingModel = null;
		this.vectorStore = null;

		chatMemoryRepository = JdbcChatMemoryRepository.builder().jdbcTemplate(vectorJdbcTemplate)
				.dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();

		chatMemory = MessageWindowChatMemory.builder().maxMessages(chatMemoryDepth)
				.chatMemoryRepository(chatMemoryRepository).build();
	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {

		String apiKey = user.getSettings().get("Anthropic API Key");
		AnthropicChatOptions chatOptions = AnthropicChatOptions.builder().apiKey(apiKey).model(assistant.model())
				.temperature(assistant.temperature()).topK(assistant.topK()).maxTokens(contextSize).build();

		AnthropicChatModel chatModel = AnthropicChatModel.builder().options(chatOptions).build();

		return ChatClient.builder(chatModel).defaultAdvisors(advisors).build();
	}

	@Override
	public ChatModel buildSimpleModel(String modelName) {
		// buildSimpleModel has no User context so it can't get an API key —
		// Anthropic models can't be used for background tasks like summarisation
		// unless you find another way to supply the key here.
		throw new UnsupportedOperationException(
				"buildSimpleModel is not supported for Anthropic — no user API key available");
	}

	@Override
	public VectorStore getVectorStore() {
		throw new UnsupportedOperationException(
				"Anthropic does not provide an embedding API — configure a separate embedding endpoint for RAG");
	}

}