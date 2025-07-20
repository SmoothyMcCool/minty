package tom.document.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.ollama.OllamaService;
import tom.task.model.Assistant;
import tom.task.services.AssistantService;
import tom.task.services.DocumentService;

@Service
public class DocumentServiceImpl implements DocumentService {

	private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

	private final ThreadPoolTaskExecutor fileProcessingExecutor;
	private final AssistantService assistantService;
	private final OllamaService ollamaService;
	private final OllamaApi ollamaApi;

	@Value("${tempFileStore}")
	private String tempFileStore;

	public DocumentServiceImpl(OllamaApi ollamaApi, AssistantService assistantService,
			@Qualifier("taskExecutor") ThreadPoolTaskExecutor fileProcessingExecutor, OllamaService ollamaService) {
		this.fileProcessingExecutor = fileProcessingExecutor;
		this.assistantService = assistantService;
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
	}

	@PostConstruct
	public void init() {
		assistantService.setDocumentService(this);
		Path docPath = Paths.get(tempFileStore);

		if (!docPath.toFile().isDirectory()) {
			logger.error(
					"DocumentServiceImpl: Specified document storage path is not a directory! Cannot monitor for files.");
			return;
		}

		File[] newFiles = docPath.toFile().listFiles();
		for (File newFile : newFiles) {
			startTaskFor(newFile.toPath());
		}

	}

	@Override
	public void processFile(File file) {
		startTaskFor(file.toPath());
	}

	@Override
	public void transformAndStore(File file, int userId, int assistantId) {
		Assistant assistant = assistantService.findAssistant(userId, assistantId);
		if (assistant.isNull()) {
			logger.warn("Tried to process a file for an assistant that does not exist or user has no permission: "
					+ file.getName());
			return;
		}

		OllamaModel model;
		try {
			model = OllamaModel.valueOf(assistant.model());
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model() + ". Cannot continue");
			return;
		}

		VectorStore vectorStore = ollamaService.getVectorStore(model);
		ChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaOptions.builder().model(model).temperature(0.9).build()).build();

		FileSystemResource resource = new FileSystemResource(file);
		List<Document> documents = read(resource);
		documents = split(documents);
		documents = enrich(documents, assistantId, chatModel);
		documents = summarize(documents, chatModel);
		vectorStore.add(documents);
	}

	private List<Document> read(Resource resource) {
		TikaDocumentReader reader = new TikaDocumentReader(resource);
		return reader.read();
	}

	private List<Document> split(List<Document> documents) {
		TokenTextSplitter splitter = new TokenTextSplitter();
		return splitter.apply(documents);
	}

	private static int NumKeywordsPerDocument = 5;

	private List<Document> enrich(List<Document> documents, int assistantId, ChatModel chatModel) {
		KeywordMetadataEnricher keywordifier = new KeywordMetadataEnricher(chatModel, NumKeywordsPerDocument);
		documents = keywordifier.apply(documents);

		for (Document document : documents) {
			document.getMetadata().put("assistantId", assistantId);
		}

		return documents;
	}

	private List<Document> summarize(List<Document> documents, ChatModel chatModel) {
		SummaryMetadataEnricher summarizer = new SummaryMetadataEnricher(chatModel,
				List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
		return summarizer.apply(documents);
	}

	private void startTaskFor(Path file) {
		String filename = file.getFileName().toString();
		int userId = userIdFromFilePath(file);

		if (userId == -1) {
			logger.error("DocumentServiceImpl: Found a file with a bad filename. Cannot process: " + filename);
			return;
		}

		int assistantId = assistantIdFromFilePath(file);
		if (assistantId == -1) {
			logger.error("DocumentServiceImpl: Found a file with a bad filename. Cannot process: " + filename);
			return;
		}

		try {
			DocumentProcessingTask dpt = new DocumentProcessingTask(file.toFile(), userId, assistantId, this,
					assistantService);
			fileProcessingExecutor.submit(dpt);
		} catch (NumberFormatException e) {
			logger.error("DocumentServiceImpl: Found a file with a bad filename. Cannot process: " + filename);
		}
	}

	@Override
	public void deleteDocumentsForAssistant(int userId, int assistantId) {

		Assistant assistant = assistantService.findAssistant(userId, assistantId);
		if (assistant.isNull()) {
			logger.warn("Tried to access an assistant that does not exist or user has no permission. User " + userId
					+ ", assistant: " + assistantId);
			return;
		}

		OllamaModel model;
		try {
			model = OllamaModel.valueOf(assistant.model());
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model() + ". Cannot continue");
			return;
		}

		VectorStore vectorStore = ollamaService.getVectorStore(model);

		vectorStore.delete(" assistantId == " + assistantId);
	}

	@Override
	public String constructFilename(int userId, int assistantId, String originalFilename) {
		return userId + "-" + assistantId + "-" + originalFilename;
	}

	private int userIdFromFilePath(Path path) {
		String filename = path.getFileName().toString();
		String[] parts = filename.split("-");
		if (parts.length < 3) {
			return -1;
		}
		try {
			return Integer.parseInt(parts[0]);
		} catch (Exception e) {
			logger.error("Could not get userId from file " + path + ". Using Null id.");
		}
		return -1;
	}

	private int assistantIdFromFilePath(Path path) {
		String filename = path.getFileName().toString();
		String[] parts = filename.split("-");
		if (parts.length < 3) {
			return -1;
		}
		try {
			return Integer.parseInt(parts[1]);
		} catch (Exception e) {
			logger.error("Could not get assistantId from file " + path + ". Using Null id.");
		}
		return -1;
	}
}
