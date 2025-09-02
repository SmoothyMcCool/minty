package tom.ollama.service;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import tom.config.ExternalProperties;

@Service
public class OllamaServiceImpl implements OllamaService {

	private static final Logger logger = LogManager.getLogger(OllamaServiceImpl.class);

	private final String ollamaChatModels;
	private final String embeddingModelName;
	private final int chatMemoryDepth;
	private final String defaultModel;

	private List<MintyOllamaModel> models;
	ModelObject modelObject;
	private final OllamaApi ollamaApi;
	private final JdbcTemplate vectorJdbcTemplate;
	private final DataSource dataSource;

	public OllamaServiceImpl(OllamaApi ollamaApi, JdbcTemplate vectorJdbcTemplate, DataSource dataSource,
			ExternalProperties properties) {
		this.ollamaApi = ollamaApi;
		this.vectorJdbcTemplate = vectorJdbcTemplate;
		this.dataSource = dataSource;
		modelObject = null;
		ollamaChatModels = properties.get("ollamaChatModels");
		embeddingModelName = properties.get("ollamaEmbeddingModel");
		chatMemoryDepth = properties.getInt("chatMemoryDepth", 20);
		defaultModel = properties.get("defaultModel");
	}

	@PostConstruct
	public void initialize() {
		models = Arrays.asList(ollamaChatModels.split(",")).stream().map(model -> MintyOllamaModel.valueOf(model))
				.toList();

		logger.info("Registering models " + models.toString());

		if (models == null) {
			logger.error("No models found!");
			return;
		}

		MintyOllamaModel embeddingModelEnum = MintyOllamaModel.valueOf(embeddingModelName);
		OllamaOptions embeddingOptions = OllamaOptions.builder().model(embeddingModelEnum.getName()).build();
		EmbeddingModel embeddingModel = new OllamaEmbeddingModel(ollamaApi, embeddingOptions, ObservationRegistry.NOOP,
				ModelManagementOptions.defaults());

		MariaDBVectorStore vectorStore = MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel)
				.schemaName("Minty").vectorTableName("vector_store").idFieldName("doc_id").contentFieldName("text")
				.metadataFieldName("meta").embeddingFieldName("embedding").initializeSchema(true).build();

		ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder().jdbcTemplate(vectorJdbcTemplate)
				.dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();

		ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(chatMemoryDepth)
				.chatMemoryRepository(chatMemoryRepository).build();

		modelObject = new ModelObject(embeddingModel, vectorStore, chatMemory, chatMemoryRepository);

	}

	@Override
	public List<MintyOllamaModel> listModels() {
		return models;
	}

	@Override
	public EmbeddingModel getEmbeddingModel() {
		return modelObject.embeddingModel();
	}

	@Override
	public VectorStore getVectorStore() {
		return modelObject.vectorStore();
	}

	@Override
	public ChatMemoryRepository getChatMemoryRepository() {
		return modelObject.chatMemoryRepository();
	}

	@Override
	public ChatMemory getChatMemory() {
		return modelObject.chatMemory();
	}

	@Override
	public MintyOllamaModel getDefaultModel() {
		return MintyOllamaModel.valueOf(defaultModel);
	}

}
