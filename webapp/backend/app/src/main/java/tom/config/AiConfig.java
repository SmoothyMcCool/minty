package tom.config;

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
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class AiConfig {

	private static final Logger logger = LogManager.getLogger(AiConfig.class);

	@Value("${ollamaUri}")
	private String ollamaUri;

	@Value("${chatMemoryDepth}")
	private int chatMemoryDepth;

	@Bean
	JdbcTemplate vectorJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	OllamaApi ollamaApi() {
		logger.info("ollama URI is " + ollamaUri);
		return OllamaApi.builder().
				baseUrl(ollamaUri)
				.build();
	}

	@Bean
	EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
		OllamaOptions options = OllamaOptions.builder()
				.model(OllamaModel.LLAMA3_2)
				.build();

		return new OllamaEmbeddingModel(ollamaApi, options, ObservationRegistry.NOOP,
				ModelManagementOptions.defaults());
	}

	@Bean
	VectorStore vectorStore(JdbcTemplate vectorJdbcTemplate, EmbeddingModel embeddingModel) {
		return MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel)
				.initializeSchema(true)
				.build();
		// return SimpleVectorStore.builder(embeddingModel).build();
	}

	@Bean
	ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder()
				.maxMessages(chatMemoryDepth)
				.chatMemoryRepository(chatMemoryRepository)
				.build();
	}

	@Bean
	ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		return JdbcChatMemoryRepository.builder()
				.jdbcTemplate(jdbcTemplate)
				.dialect(JdbcChatMemoryRepositoryDialect.from(dataSource))
				.build();
		// return new InMemoryChatMemoryRepository();
	}
}
