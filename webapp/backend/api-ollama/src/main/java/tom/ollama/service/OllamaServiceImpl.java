package tom.ollama.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.micrometer.observation.ObservationRegistry;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.MintyConfiguration;
import tom.config.model.ChatModelConfig;
import tom.llm.service.LlmService;
import tom.tool.auditing.AuditingToolCallingManager;

// NO Service annotation. This bean is instantiated dynamically based on the LLM engine being used.
public class OllamaServiceImpl implements LlmService {

	private static final Logger logger = LogManager.getLogger(OllamaServiceImpl.class);

	private final List<ChatModelConfig> modelDefinitions;
	private final List<String> activeModels;
	private final OllamaApi ollamaApi;
	private final MariaDBVectorStore vectorStore;
	private final ChatMemoryRepository chatMemoryRepository;
	private final ChatMemory chatMemory;
	private final EmbeddingModel embeddingModel;
	private final ToolCallingManager defaultToolCallingManager;

	public OllamaServiceImpl(OllamaApi ollamaApi, JdbcTemplate vectorJdbcTemplate, DataSource dataSource,
			MintyConfiguration properties) {
		String embeddingModelName = properties.getConfig().llm().embedding().model();
		int chatMemoryDepth = properties.getConfig().llm().chatMemoryDepth();

		modelDefinitions = properties.getConfig().llm().modelDefinitions();
		activeModels = properties.getConfig().llm().activeModels();

		try {
			logger.info("Ollama Interface Service starting...");
			ObjectMapper mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			logger.info("Defined models " + mapper.writeValueAsString(modelDefinitions));
			logger.info("Active models " + mapper.writeValueAsString(activeModels));
		} catch (JsonProcessingException e) {
			// Just ignore. Startup logging. If this didn't work we got an exception for a
			// malformed file before this anyway.
		}

		this.ollamaApi = ollamaApi;

		OllamaEmbeddingOptions embeddingOptions = OllamaEmbeddingOptions.builder().model(embeddingModelName).build();
		embeddingModel = new OllamaEmbeddingModel(ollamaApi, embeddingOptions, ObservationRegistry.NOOP,
				ModelManagementOptions.defaults());

		vectorStore = MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel).schemaName("Minty")
				.vectorTableName("vector_store").idFieldName("doc_id").contentFieldName("text")
				.metadataFieldName("meta").embeddingFieldName("embedding").initializeSchema(true).build();

		chatMemoryRepository = JdbcChatMemoryRepository.builder().jdbcTemplate(vectorJdbcTemplate)
				.dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();

		chatMemory = MessageWindowChatMemory.builder().maxMessages(chatMemoryDepth)
				.chatMemoryRepository(chatMemoryRepository).build();

		defaultToolCallingManager = DefaultToolCallingManager.builder().build();
	}

	@Override
	public List<ChatModelConfig> listModels() {
		return modelDefinitions.stream().filter(model -> activeModels.contains(model.name())).toList();
	}

	@Override
	public VectorStore getVectorStore() {
		return vectorStore;
	}

	@Override
	public ChatMemoryRepository getChatMemoryRepository() {
		return chatMemoryRepository;
	}

	@Override
	public ChatMemory getChatMemory() {
		return chatMemory;
	}

	@Override
	public boolean isModelValid(String model) {
		try {
			OllamaModel.valueOf(model);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int estimateInputTokens(AssistantQuery query) {
		TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();
		AtomicInteger chatHistorySize = new AtomicInteger(); // Just to be usable in the lambda below.

		chatMemory.get(query.getConversationId().value().toString())
				.forEach(msg -> chatHistorySize.getAndAdd(tokenCountEstimator.estimate(msg.getText())));

		int querySize = tokenCountEstimator.estimate(query.getQuery());
		int inputTokens = chatHistorySize.get() + querySize;
		return inputTokens;
	}

	@Override
	public ChatClient buildChatClient(Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {
		OllamaChatOptions chatOptions = OllamaChatOptions.builder().model(assistant.model())
				.temperature(assistant.temperature()).numCtx(contextSize).build();

		ChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.toolCallingManager(new AuditingToolCallingManager(query.getConversationId().getValue().toString(),
						defaultToolCallingManager))
				.defaultOptions(chatOptions).build();

		return ChatClient.builder(chatModel).defaultAdvisors(advisors).defaultOptions(chatOptions).build();

	}

	@Override
	public ChatModel buildSimpleModel(String model) {
		return OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaChatOptions.builder().model(model).build()).build();
	}

}
