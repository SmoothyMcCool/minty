package tom.openai.service;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.ChatClient;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.credential.BearerTokenCredential;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.EndpointConfig;
import tom.llm.service.LlmEndpointService;
import tom.tool.auditing.AuditingToolCallingManager;
import tom.user.model.User;

public class OpenAiEndpointService implements LlmEndpointService {

	private final OpenAIClient openAiClient;
	private final OpenAIClientAsync openAiClientAsync;
	private final VectorStore vectorStore;
	private final EmbeddingModel embeddingModel;
	private final ToolCallingManager defaultToolCallingManager;

	public OpenAiEndpointService(EndpointConfig endpointConfig, JdbcTemplate vectorJdbcTemplate, DataSource dataSource,
			String embeddingModelName, int chatMemoryDepth) {

		this.openAiClient = OpenAIOkHttpClient.builder().baseUrl(endpointConfig.url().toString())
				.apiKey("doesn't matter for llama-cpp").timeout(endpointConfig.apiTimeout()).build();

		this.openAiClientAsync = OpenAIOkHttpClientAsync.builder().baseUrl(endpointConfig.url().toString())
				.apiKey("doesn't matter for llama.cpp").timeout(endpointConfig.apiTimeout()).build();

		OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder().model(embeddingModelName).build();
		embeddingModel = new OpenAiEmbeddingModel(openAiClient, MetadataMode.EMBED, embeddingOptions);

		vectorStore = MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel).schemaName("Minty")
				.vectorTableName("vector_store").idFieldName("doc_id").contentFieldName("text")
				.metadataFieldName("meta").embeddingFieldName("embedding").initializeSchema(true).build();

		defaultToolCallingManager = DefaultToolCallingManager.builder().build();
	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {
		// String apiKey = user.getSettings().get("OpenAI Key");
		// if (apiKey == null) {
		// throw new RuntimeException("No OpenAI key set up.");
		// }
		OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().model(assistant.model())
				.credential(BearerTokenCredential.create("doesn't matter for llama-cpp"))
				.temperature(assistant.temperature())
				// OpenAI-compat endpoints don't always honour topK, but pass it through
				.build();

		ChatModel chatModel = OpenAiChatModel.builder().openAiClient(openAiClient).openAiClientAsync(openAiClientAsync)
				.toolCallingManager(new AuditingToolCallingManager(query.getConversationId().getValue().toString(),
						defaultToolCallingManager))
				.options(chatOptions).build();

		return ChatClient.builder(chatModel).defaultAdvisors(advisors).build();
	}

	@Override
	public ChatModel buildSimpleModel(String modelName) {
		return OpenAiChatModel.builder().openAiClient(openAiClient).openAiClientAsync(openAiClientAsync)
				.options(OpenAiChatOptions.builder().model(modelName).build()).build();
	}

	@Override
	public VectorStore getVectorStore() {
		return vectorStore;
	}

}