package tom.ollama.service;

import java.util.Arrays;
import java.util.Collections;
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
import tom.config.ExternalProperties;

@Service
public class OllamaServiceImpl implements OllamaService {

	private static final Logger logger = LogManager.getLogger(OllamaServiceImpl.class);

	private final List<String> models;
	private final MariaDBVectorStore vectorStore;
	private final ChatMemoryRepository chatMemoryRepository;
	private final ChatMemory chatMemory;
	private final EmbeddingModel embeddingModel;
	private final String defaultModel;

	public OllamaServiceImpl(OllamaApi ollamaApi, JdbcTemplate vectorJdbcTemplate, DataSource dataSource,
			ExternalProperties properties) {
		String embeddingModelName = properties.get("ollamaEmbeddingModel");
		int chatMemoryDepth = properties.getInt("chatMemoryDepth", 20);
		defaultModel = properties.get("defaultModel");

		String modelsStr = properties.get("ollamaChatModels");
		if (modelsStr == null || modelsStr.trim().isEmpty()) {
			throw new IllegalArgumentException("ollamaChatModels must not be null or empty");
		}
		models = Arrays.asList(modelsStr.split(","));

		logger.info("Registering models " + models.toString());

		OllamaOptions embeddingOptions = OllamaOptions.builder().model(embeddingModelName).build();
		embeddingModel = new OllamaEmbeddingModel(ollamaApi, embeddingOptions, ObservationRegistry.NOOP,
				ModelManagementOptions.defaults());

		vectorStore = MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel).schemaName("Minty")
				.vectorTableName("vector_store").idFieldName("doc_id").contentFieldName("text")
				.metadataFieldName("meta").embeddingFieldName("embedding").initializeSchema(true).build();

		chatMemoryRepository = JdbcChatMemoryRepository.builder().jdbcTemplate(vectorJdbcTemplate)
				.dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();

		chatMemory = MessageWindowChatMemory.builder().maxMessages(chatMemoryDepth)
				.chatMemoryRepository(chatMemoryRepository).build();
	}

	@Override
	public List<String> listModels() {
		return Collections.unmodifiableList(models);
	}

	@Override
	public EmbeddingModel getEmbeddingModel() {
		return embeddingModel;
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
	public String getDefaultModel() {
		return defaultModel;
	}

}
