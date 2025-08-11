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
import jakarta.transaction.Transactional;
import tom.document.model.DocumentState;
import tom.document.model.MintyDoc;
import tom.document.repository.DocumentRepository;
import tom.ollama.service.MintyOllamaModel;
import tom.ollama.service.OllamaService;

@Service
public class DocumentServiceImpl implements DocumentService {

	private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

	private final ThreadPoolTaskExecutor fileProcessingExecutor;
	private final OllamaService ollamaService;
	private final AssistantDocumentLinkService assistantDocumentLinkService;
	private final OllamaApi ollamaApi;
	private final DocumentRepository documentRepository;

	@Value("${docFileStore}")
	private String docFileStore;

	@Value("${ollamaSummarizingModel}")
	private String summarizingModel;

	public DocumentServiceImpl(OllamaApi ollamaApi, DocumentRepository documentRepository,
			@Qualifier("fileProcessingExecutor") ThreadPoolTaskExecutor fileProcessingExecutor,
			OllamaService ollamaService, AssistantDocumentLinkService assistantDocumentLinkService) {
		this.fileProcessingExecutor = fileProcessingExecutor;
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
		this.documentRepository = documentRepository;
		this.assistantDocumentLinkService = assistantDocumentLinkService;
	}

	@PostConstruct
	public void init() {
		Path docPath = Paths.get(docFileStore);

		if (!docPath.toFile().isDirectory()) {
			logger.error(
					"DocumentServiceImpl: Specified document storage path is not a directory! Cannot monitor for files.");
			return;
		}

		File[] newFiles = docPath.toFile().listFiles();
		for (File newFile : newFiles) {
			String documentId = newFile.getName();
			startTaskFor(newFile.toPath(), documentId);
		}

	}

	@Override
	public void processFile(File file) {
		String documentId = file.getName();
		startTaskFor(file.toPath(), documentId);
	}

	@Override
	public void transformAndStore(File file, MintyDoc doc) {
		MintyOllamaModel model = MintyOllamaModel.valueOf(summarizingModel);

		VectorStore vectorStore = ollamaService.getVectorStore();
		ChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaOptions.builder().model(model.id()).build()).build();

		FileSystemResource resource = new FileSystemResource(file);
		List<Document> documents = read(resource);
		documents = split(documents);
		documents = enrich(doc.getDocumentId(), documents, chatModel);
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

	private List<Document> enrich(String documentId, List<Document> documents, ChatModel chatModel) {
		KeywordMetadataEnricher keywordifier = new KeywordMetadataEnricher(chatModel, NumKeywordsPerDocument);
		documents = keywordifier.apply(documents);

		for (Document document : documents) {
			document.getMetadata().put("documentId", documentId);
		}

		return documents;
	}

	private List<Document> summarize(List<Document> documents, ChatModel chatModel) {
		SummaryMetadataEnricher summarizer = new SummaryMetadataEnricher(chatModel,
				List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
		return summarizer.apply(documents);
	}

	private void startTaskFor(Path file, String documentId) {
		String filename = file.getFileName().toString();
		MintyDoc doc = this.findByDocumentId(documentId);

		if (doc == null) {
			logger.warn("Attempted to start a task for a document that does not exist!");
			return;
		}

		try {
			DocumentProcessingTask dpt = new DocumentProcessingTask(file.toFile(), this);
			fileProcessingExecutor.submit(dpt);
		} catch (NumberFormatException e) {
			logger.error("DocumentServiceImpl: Found a file with a bad filename. Cannot process: " + filename);
		}
	}

	@Override
	@Transactional
	public boolean deleteDocument(int userId, String documentId) {

		MintyDoc document = findByDocumentId(documentId);
		if (document.getOwnerId() != userId) {
			logger.warn("User " + userId + " tried to delete unowned document " + documentId);
			return false;
		}

		VectorStore vectorStore = ollamaService.getVectorStore();
		vectorStore.delete(" documentId == \"" + documentId + "\"");

		documentRepository.deleteByDocumentId(documentId);

		return true;
	}

	@Override
	public void markDocumentComplete(MintyDoc doc) {
		doc.setState(DocumentState.READY);
		documentRepository.save(doc);
	}

	@Override
	public List<MintyDoc> listDocuments() {
		List<MintyDoc> docs = documentRepository.findAll();
		docs.forEach(doc -> {
			doc.setAssociatedAssistants(assistantDocumentLinkService.getAssistantIdsForDocument(doc.getDocumentId()));
		});
		return docs;
	}

	@Override
	public boolean documentExists(String documentId) {
		return documentRepository.existsByDocumentId(documentId);
	}

	@Override
	public MintyDoc addDocument(int userId, MintyDoc document) {
		document.setOwnerId(userId);
		document.setState(DocumentState.NO_CONTENT);
		return documentRepository.save(document);
	}

	@Override
	public boolean documentOwnedBy(int userId, String documentId) {
		MintyDoc doc = documentRepository.findByDocumentId(documentId);
		return userId == doc.getOwnerId();
	}

	@Override
	public MintyDoc findByDocumentId(String documentId) {
		return documentRepository.findByDocumentId(documentId);
	}

}
