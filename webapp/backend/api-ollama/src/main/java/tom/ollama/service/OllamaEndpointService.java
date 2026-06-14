package tom.ollama.service;

import java.util.List;

import javax.sql.DataSource;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import io.micrometer.observation.ObservationRegistry;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.EndpointConfig;
import tom.llm.http.HttpClientFactory;
import tom.llm.service.LlmEndpointService;
import tom.tool.auditing.AuditingToolCallingManager;
import tom.user.model.User;

public class OllamaEndpointService implements LlmEndpointService {

	private final OllamaApi ollamaApi;
	private final CloseableHttpClient httpClient;
	private final VectorStore vectorStore;
	private final EmbeddingModel embeddingModel;
	private final ToolCallingManager defaultToolCallingManager;

	public OllamaEndpointService(EndpointConfig endpointConfig, JdbcTemplate vectorJdbcTemplate, DataSource dataSource,
			String embeddingModelName, int chatMemoryDepth) {

		this.httpClient = HttpClientFactory.build(endpointConfig.apiConnectionTimeout(), endpointConfig.apiTimeout(),
				50);

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

		this.ollamaApi = OllamaApi.builder().baseUrl(endpointConfig.url().toString())
				.restClientBuilder(restClientBuilder).build();

		OllamaEmbeddingOptions embeddingOptions = OllamaEmbeddingOptions.builder().model(embeddingModelName).build();
		embeddingModel = new OllamaEmbeddingModel(ollamaApi, embeddingOptions, ObservationRegistry.NOOP,
				ModelManagementOptions.defaults());

		vectorStore = MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel).schemaName("Minty")
				.vectorTableName("vector_store").idFieldName("doc_id").contentFieldName("text")
				.metadataFieldName("meta").embeddingFieldName("embedding").initializeSchema(true).build();

		defaultToolCallingManager = DefaultToolCallingManager.builder().build();
	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {
		OllamaChatOptions chatOptions = OllamaChatOptions.builder().model(assistant.model())
				.temperature(assistant.temperature()).numCtx(contextSize).topK(assistant.topK()).build();

		ChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.toolCallingManager(new AuditingToolCallingManager(query.getConversationId().getValue().toString(),
						defaultToolCallingManager))
				.defaultOptions(chatOptions).build();

		return ChatClient.builder(chatModel).defaultAdvisors(advisors).build();
	}

	@Override
	public ChatModel buildSimpleModel(String modelName) {
		return OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaChatOptions.builder().model(modelName).build()).build();
	}

	@Override
	public VectorStore getVectorStore() {
		return vectorStore;
	}

}