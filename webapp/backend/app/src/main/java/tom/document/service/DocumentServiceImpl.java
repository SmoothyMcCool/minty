package tom.document.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import tom.assistant.repository.AssistantRepository;
import tom.task.services.DocumentService;

@Service
public class DocumentServiceImpl implements DocumentService {

	private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

	private final VectorStore vectorStore;
	private final OllamaChatModel chatModel;
	private final ThreadPoolTaskExecutor fileProcessingExecutor;
	private final AssistantRepository assistantRepository;

	@Value("${tempFileStore}")
	private String tempFileStore;

	public DocumentServiceImpl(OllamaApi ollamaApi, VectorStore vectorStore, AssistantRepository assistantRepository,
			@Qualifier("taskExecutor") ThreadPoolTaskExecutor fileProcessingExecutor) {
		this.chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaOptions.builder().model(OllamaModel.LLAMA3_2).temperature(0.9).build()).build();
		this.vectorStore = vectorStore;
		this.fileProcessingExecutor = fileProcessingExecutor;
		this.assistantRepository = assistantRepository;
	}

	@PostConstruct
	public void init() {
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
	public void transformAndStore(File file, int assistantId) {
		FileSystemResource resource = new FileSystemResource(file);
		List<Document> documents = read(resource);
		documents = split(documents);
		documents = enrich(documents, assistantId);
		documents = summarize(documents);
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

	private List<Document> enrich(List<Document> documents, int assistantId) {
		KeywordMetadataEnricher keywordifier = new KeywordMetadataEnricher(chatModel, NumKeywordsPerDocument);
		documents = keywordifier.apply(documents);

		for (Document document : documents) {
			document.getMetadata().put("assistantId", assistantId);
		}

		return documents;
	}

	private List<Document> summarize(List<Document> documents) {
		SummaryMetadataEnricher summarizer = new SummaryMetadataEnricher(chatModel,
				List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
		return summarizer.apply(documents);
	}

	private void startTaskFor(Path file) {
		String filename = file.getFileName().toString();
		String[] parts = filename.split("-");

		try {
			if (parts.length <= 1) {
				logger.error("DocumentServiceImpl: Found a file with a bad filename. Cannot process: " + filename);
				return;
			}
			int assistantId = Integer.parseInt(parts[0]);
			DocumentProcessingTask dpt = new DocumentProcessingTask(file.toFile(), assistantId, assistantRepository,
					this);
			fileProcessingExecutor.submit(dpt);
		} catch (NumberFormatException e) {
			logger.error("DocumentServiceImpl: Found a file with a bad filename. Cannot process: " + filename);
		}
	}

	@Override
	public void deleteDocumentsForAssistant(int assistantId) {
		this.vectorStore.delete(" assistantId == " + assistantId);
	}

}
