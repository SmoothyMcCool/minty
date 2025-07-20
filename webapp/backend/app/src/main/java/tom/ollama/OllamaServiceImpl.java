package tom.ollama;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;

@Service
public class OllamaServiceImpl implements OllamaService {

	private static final Logger logger = LogManager.getLogger(OllamaServiceImpl.class);

	@Value("${ollamaModels}")
	private String ollamaModels;

	@Value("${chatMemoryDepth}")
	private int chatMemoryDepth;

	@Value("${defaultModel}")
	private String defaultModel;

	private List<OllamaModel> models;
	private Map<OllamaModel, ModelObject> modelObjects;
	private final OllamaApi ollamaApi;
	private final JdbcTemplate vectorJdbcTemplate;
	private final DataSource dataSource;

	public OllamaServiceImpl(OllamaApi ollamaApi, JdbcTemplate vectorJdbcTemplate, DataSource dataSource) {
		this.ollamaApi = ollamaApi;
		this.vectorJdbcTemplate = vectorJdbcTemplate;
		this.dataSource = dataSource;
		modelObjects = new HashMap<>();
	}

	@PostConstruct
	public void initialize() {
		models = Arrays.asList(ollamaModels.split(",")).stream().map(model -> OllamaModel.valueOf(model)).toList();

		if (models == null) {
			logger.error("No models found!");
			return;
		}

		models.forEach(model -> {
			logger.info("Registering model " + model);
			OllamaOptions options = OllamaOptions.builder().model(model).build();

			EmbeddingModel embeddingModel = new OllamaEmbeddingModel(ollamaApi, options, ObservationRegistry.NOOP,
					ModelManagementOptions.defaults());

			VectorStore vectorStore = MariaDBVectorStore.builder(vectorJdbcTemplate, embeddingModel)
					.initializeSchema(true).build();

			ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
					.jdbcTemplate(vectorJdbcTemplate).dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();

			ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(chatMemoryDepth)
					.chatMemoryRepository(chatMemoryRepository).build();

			modelObjects.put(model, new ModelObject(embeddingModel, vectorStore, chatMemory, chatMemoryRepository));

		});
	}

	@Override
	public List<OllamaModel> listModels() {
		return models;
	}

	@Override
	public EmbeddingModel getEmbeddingModel(OllamaModel model) {
		return modelObjects.get(model).embeddingModel();
	}

	@Override
	public VectorStore getVectorStore(OllamaModel model) {
		return modelObjects.get(model).vectorStore();
	}

	@Override
	public ChatMemoryRepository getChatMemoryRepository(OllamaModel model) {
		return modelObjects.get(model).chatMemoryRepository();
	}

	@Override
	public ChatMemory getChatMemory(OllamaModel model) {
		return modelObjects.get(model).chatMemory();
	}

	@Override
	public OllamaModel getDefaultModel() {
		return OllamaModel.valueOf(defaultModel);
	}

}
