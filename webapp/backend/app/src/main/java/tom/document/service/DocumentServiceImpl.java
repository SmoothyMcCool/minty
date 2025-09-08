package tom.document.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import tom.config.ExternalProperties;
import tom.document.model.DocumentState;
import tom.document.model.MintyDoc;
import tom.document.repository.DocumentRepository;
import tom.ollama.service.OllamaService;

@Service
public class DocumentServiceImpl implements DocumentService {

	private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

	private final ThreadPoolTaskExecutor fileProcessingExecutor;
	private final OllamaService ollamaService;
	private final AssistantDocumentLinkService assistantDocumentLinkService;
	private final OllamaApi ollamaApi;
	private final DocumentRepository documentRepository;
	private final int keywordsPerDocument;

	private final String docFileStore;
	private final String summarizingModel;

	public DocumentServiceImpl(OllamaApi ollamaApi, DocumentRepository documentRepository,
			@Qualifier("fileProcessingExecutor") ThreadPoolTaskExecutor fileProcessingExecutor,
			OllamaService ollamaService, AssistantDocumentLinkService assistantDocumentLinkService,
			ExternalProperties properties) {
		this.fileProcessingExecutor = fileProcessingExecutor;
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
		this.documentRepository = documentRepository;
		this.assistantDocumentLinkService = assistantDocumentLinkService;
		summarizingModel = properties.get("ollamaSummarizingModel");
		docFileStore = properties.get("docFileStore");
		keywordsPerDocument = properties.getInt("keywordsPerDocument", 5);
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

		if (newFiles == null) {
			logger.error("Could not list files in " + docFileStore);
			return;
		}

		for (File newFile : newFiles) {
			processFile(newFile);
		}

	}

	@PreDestroy
	public void shutdown() {
		fileProcessingExecutor.initiateShutdown();
	}

	@Override
	public void processFile(File file) {
		try {
			if (file.isFile()) {
				UUID documentId = UUID.fromString(file.getName());
				startTaskFor(file.toPath(), documentId);
			}
		} catch (IllegalArgumentException e) {
			logger.error("Invalid UUID filename! Deleting file " + file.getName(), e);
			file.delete();
		} catch (Exception e) {
			logger.error("Could not start processing task for file " + file.getName(), e);
		}
	}

	@Override
	public void transformAndStore(File file, MintyDoc doc) {
		VectorStore vectorStore = ollamaService.getVectorStore();
		ChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaOptions.builder().model(summarizingModel).build()).build();

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

	private List<Document> enrich(UUID documentId, List<Document> documents, ChatModel chatModel) {
		KeywordMetadataEnricher keywordifier = new KeywordMetadataEnricher(chatModel, keywordsPerDocument);
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

	private void startTaskFor(Path file, UUID documentId) {
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
	public boolean deleteDocument(UUID userId, UUID documentId) {

		MintyDoc document = findByDocumentId(documentId);

		if (document == null) {
			logger.warn("Document " + documentId + " does not exist!");
			return false;
		}

		if (!document.getOwnerId().equals(userId)) {
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
	public boolean documentExists(UUID documentId) {
		return documentRepository.existsByDocumentId(documentId);
	}

	@Override
	@Transactional
	public MintyDoc addDocument(UUID userId, MintyDoc document) {
		document.setDocumentId(null);
		document.setOwnerId(userId);
		document.setState(DocumentState.NO_CONTENT);
		return documentRepository.save(document);
	}

	@Override
	public boolean documentOwnedBy(UUID userId, UUID documentId) {
		MintyDoc doc = documentRepository.findByDocumentId(documentId);
		return doc != null && userId.equals(doc.getOwnerId());
	}

	@Override
	public MintyDoc findByDocumentId(UUID documentId) {
		return documentRepository.findByDocumentId(documentId);
	}

}
