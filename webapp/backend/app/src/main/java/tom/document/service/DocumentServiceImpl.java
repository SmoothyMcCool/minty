package tom.document.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import tom.api.DocumentId;
import tom.api.UserId;
import tom.config.MintyConfiguration;
import tom.document.model.DocumentState;
import tom.document.model.MintyDoc;
import tom.document.repository.DocumentRepository;
import tom.llm.service.LlmService;

@Service
public class DocumentServiceImpl implements DocumentServiceInternal {

	private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

	private final ThreadPoolTaskExecutor fileProcessingExecutor;
	private final LlmService llmService;
	private final AssistantDocumentLinkService assistantDocumentLinkService;
	private final DocumentRepository documentRepository;
	private final JdbcTemplate vectorJdbcTemplate;
	private final int keywordsPerDocument;
	private final int documentTargetChunkSize;
	private final int macroTargetChunkSize;
	private final int embeddingBatchSize;
	// private final int maxEmbeddingTokens;

	private final Path docFileStore;
	private final ChatModel chatModel;

	public DocumentServiceImpl(DocumentRepository documentRepository,
			@Qualifier("fileProcessingExecutor") ThreadPoolTaskExecutor fileProcessingExecutor, LlmService llmService,
			AssistantDocumentLinkService assistantDocumentLinkService, MintyConfiguration properties,
			JdbcTemplate vectorJdbcTemplate) {
		this.fileProcessingExecutor = fileProcessingExecutor;
		this.llmService = llmService;
		this.documentRepository = documentRepository;
		this.assistantDocumentLinkService = assistantDocumentLinkService;
		this.vectorJdbcTemplate = vectorJdbcTemplate;
		docFileStore = properties.getConfig().fileStores().docs();
		keywordsPerDocument = properties.getConfig().llm().embedding().keywordsPerDocument();
		documentTargetChunkSize = properties.getConfig().llm().embedding().documentTargetChunkSize();
		macroTargetChunkSize = properties.getConfig().llm().embedding().macroTargetChunkSize();
		embeddingBatchSize = properties.getConfig().llm().embedding().batchSize();
		// maxEmbeddingTokens =
		// properties.getConfig().llm().embedding().maxEmbeddingTokens();

		String summarizingModel = properties.getConfig().llm().embedding().summarizingModel();
		chatModel = llmService.buildSimpleModel(summarizingModel);

	}

	@PostConstruct
	public void init() {
		if (!docFileStore.toFile().isDirectory()) {
			logger.error(
					"DocumentServiceImpl: Specified document storage path is not a directory! Cannot monitor for files.");
			return;
		}

		File[] newFiles = docFileStore.toFile().listFiles();

		if (newFiles == null) {
			logger.error("Could not list files in " + docFileStore);
			return;
		}

		for (File newFile : newFiles) {
			logger.info("Starting processing of document " + newFile.getName());
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
				DocumentId documentId = new DocumentId(UUID.fromString(file.getName()));
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
		FileSystemResource resource = new FileSystemResource(file);
		List<Document> rawDocuments = read(resource);

		// Summarize larger sections first
		List<Document> macroChunks = split(rawDocuments, macroTargetChunkSize);
		macroChunks = normalize(macroChunks);
		macroChunks = summarize(macroChunks, chatModel);
		macroChunks = enrich(doc.getDocumentId(), macroChunks, chatModel);

		// Now split into small chunks for embedding
		List<Document> microChunks = split(macroChunks, documentTargetChunkSize);
		for (Document document : microChunks) {
			document.getMetadata().put("documentId", doc.getDocumentId());
			document.getMetadata().put("source", doc.getTitle());
		}

		for (int i = 0; i < microChunks.size(); i += embeddingBatchSize) {
			List<Document> batch = microChunks.subList(i, Math.min(i + embeddingBatchSize, microChunks.size()));
			if (!addDocuments(batch, documentTargetChunkSize, 0)) {
				String batchStr = batch.stream().map(d -> d.getText()).collect(Collectors.joining("\n-----\n"));
				throw new DocumentProcessingException(
						"Failed to store document despite repeated re-chunking. Failing. The failing chunk was: "
								+ batchStr);
			}
		}
	}

	private List<Document> read(Resource resource) {
		TikaDocumentReader reader = new TikaDocumentReader(resource);
		return reader.read();
	}

	private List<Document> split(List<Document> documents, int targetChunkSize) {
		TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(targetChunkSize).build();
		return splitter.apply(documents);
	}

	private List<Document> normalize(List<Document> documents) {
		return documents.stream().map(doc -> {
			String text = doc.getText();

			if (StringUtils.isBlank(text)) {
				return null;
			}

			@SuppressWarnings("null")
			String clean = text.replaceAll("\\s+", " ").replaceAll("data:image/[^;]+;base64,[A-Za-z0-9+/=]+", "[B64]")
					.replaceAll("\\{[^}]{1000,}\\}", "[JSON_BLOCK]");

			return new Document(clean, doc.getMetadata());
		}).filter(Objects::nonNull).toList();

	}

	private List<Document> enrich(DocumentId documentId, List<Document> documents, ChatModel chatModel) {
		KeywordMetadataEnricher keywordifier = new KeywordMetadataEnricher(chatModel, keywordsPerDocument);
		List<Document> keywordedDocuments = keywordifier.apply(documents);

		return keywordedDocuments;
	}

	private List<Document> summarize(List<Document> documents, ChatModel chatModel) {
		SummaryMetadataEnricher summarizer = new SummaryMetadataEnricher(chatModel, List.of(SummaryType.CURRENT));
		// List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
		return summarizer.apply(documents);
	}

	private boolean addDocuments(List<Document> documents, int targetChunkSize, int iteration) {

		if (iteration >= 5) {
			return false;
		}

		VectorStore vectorStore = llmService.getVectorStore();
		try {
			vectorStore.add(documents);
			return true;
		} catch (Exception e) {
			logger.warn("Chunk too large. Splitting.");
			// split again and retry
			int newTarget = targetChunkSize / 2;
			return addDocuments(split(documents, newTarget), newTarget, iteration + 1);
		}
	}

	private void startTaskFor(Path file, DocumentId documentId) {
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
	public boolean deleteDocument(UserId userId, DocumentId documentId) {

		MintyDoc document = findByDocumentId(documentId);

		if (document == null) {
			logger.warn("Document " + documentId + " does not exist!");
			return false;
		}

		if (!document.getOwnerId().equals(userId)) {
			logger.warn("User " + userId + " tried to delete unowned document " + documentId);
			return false;
		}

		// Using the VectorStore delete method doesn't work as Spring AI doesn't seem to
		// be able to successfully query by the documentId metadata that was added.
		vectorJdbcTemplate.update("DELETE FROM Minty.vector_store WHERE JSON_VALUE(meta, '$.documentId') = ?",
				documentId.getValue().toString());

		documentRepository.deleteById(documentId.value());

		return true;
	}

	@Override
	public void markDocumentComplete(MintyDoc doc) {
		doc.setState(DocumentState.READY);
		documentRepository.save(doc);
	}

	@Override
	public void markDocumentFailed(MintyDoc doc) {
		doc.setState(DocumentState.FAILED);
		documentRepository.save(doc);
	}

	@Override
	public List<MintyDoc> listDocuments(UserId userId) {
		List<MintyDoc> docs = documentRepository.findAllByOwnerId(userId);
		docs.forEach(doc -> {
			doc.setAssociatedAssistants(assistantDocumentLinkService.getAssistantIdsForDocument(doc.getDocumentId()));
		});
		return docs;
	}

	@Override
	public boolean documentExists(MintyDoc document) {
		return documentRepository.existsByTitle(document.getTitle());
	}

	@Override
	@Transactional
	public MintyDoc addDocument(UserId userId, MintyDoc document) {
		document.setDocumentId(null);
		document.setOwnerId(userId);
		document.setState(DocumentState.NO_CONTENT);
		return documentRepository.save(document);
	}

	@Override
	public boolean documentOwnedBy(UserId userId, DocumentId documentId) {
		Optional<MintyDoc> doc = documentRepository.findById(documentId.value());
		return doc.isPresent() && userId.equals(doc.get().getOwnerId());
	}

	@Override
	public MintyDoc findByDocumentId(DocumentId documentId) {
		return documentRepository.findById(documentId.value()).orElse(null);
	}

	@Override
	public String fileBytesToText(byte[] bytes) {
		Tika tika = new Tika();
		tika.detect(bytes);
		try {
			return tika.parseToString(new ByteArrayInputStream(bytes));
		} catch (IOException | TikaException e) {
			throw new RuntimeException("Failed to parse file.", e);
		}
	}
}
