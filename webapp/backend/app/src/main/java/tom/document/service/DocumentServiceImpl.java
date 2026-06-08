package tom.document.service;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import tom.api.DocumentId;
import tom.api.DocumentSectionId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSection;
import tom.api.model.document.SpreadsheetFormat;
import tom.api.services.DocumentExtractorService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.config.MintyConfiguration;
import tom.conversation.service.ConversationServiceInternal;
import tom.document.model.MintyDoc;
import tom.document.model.MintyDocSegment;
import tom.document.repository.DocumentRepository;
import tom.document.repository.DocumentSegmentRepository;
import tom.document.service.tasks.DecomposedMarkdownDocumentProcessingTask;

@Service
public class DocumentServiceImpl implements DocumentServiceInternal {

	private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);

	private final ThreadPoolTaskExecutor fileProcessingExecutor;
	private final DocumentRepository documentRepository;
	private final DocumentSegmentRepository documentSegmentRepository;
	private final JdbcTemplate vectorJdbcTemplate;
	private final ConversationServiceInternal conversationService;
	private final AssistantManagementService assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final DocumentExtractorService documentExtractorService;
	private final MintyConfiguration config;

	private List<DecomposedMarkdownDocumentProcessingTask> inProgressTasks;
	private List<DecomposedMarkdownDocumentProcessingTask> completedTasks;

	public DocumentServiceImpl(DocumentRepository documentRepository,
			DocumentSegmentRepository documentSegmentRepository,
			@Qualifier("fileProcessingExecutor") ThreadPoolTaskExecutor fileProcessingExecutor,
			MintyConfiguration properties, JdbcTemplate vectorJdbcTemplate,
			ConversationServiceInternal conversationService, AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, DocumentExtractorService documentExtractorService,
			MintyConfiguration config) {
		this.fileProcessingExecutor = fileProcessingExecutor;
		this.documentRepository = documentRepository;
		this.documentSegmentRepository = documentSegmentRepository;
		this.vectorJdbcTemplate = vectorJdbcTemplate;
		this.conversationService = conversationService;
		this.assistantManagementService = assistantManagementService;
		this.assistantQueryService = assistantQueryService;
		this.documentExtractorService = documentExtractorService;
		this.config = config;

		inProgressTasks = Collections.synchronizedList(new ArrayList<>());
		completedTasks = Collections.synchronizedList(new ArrayList<>());
	}

	@PreDestroy
	public void shutdown() {
		fileProcessingExecutor.initiateShutdown();
	}

	@Override
	public String fileToMarkdown(File file, SpreadsheetFormat format) {
		try {
			return documentExtractorService.extract(file, format);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse file.", e);
		}
	}

	@Override
	public void fileToMarkdownAndDecompose(UserId userId, ProjectId projectId, File file, boolean summarize)
			throws Exception {

		String markdown = documentExtractorService.extract(file);
		String filename = file.getName();
		int dotIndex = filename.lastIndexOf('.');
		filename = (dotIndex != -1) ? filename.substring(0, dotIndex) : filename + ".md";

		DecomposedMarkdownDocumentProcessingTask task = new DecomposedMarkdownDocumentProcessingTask(userId, projectId,
				filename, markdown, conversationService, assistantManagementService, assistantQueryService,
				documentExtractorService, this, config, summarize);

		synchronized (inProgressTasks) {
			inProgressTasks.add(task);
			fileProcessingExecutor.submit(task);
		}

	}

	@Override
	public List<String> getInProgressTaskNames(UserId userId) {
		synchronized (inProgressTasks) {
			List<String> tasks = inProgressTasks.stream().filter(task -> task.getUserId().equals(userId))
					.map(task -> task.getFilename() + "(In Progress)").collect(Collectors.toCollection(ArrayList::new));
			tasks.addAll(completedTasks.stream().filter(task -> task.getUserId().equals(userId))
					.map(task -> task.getFilename() + "(Complete)").toList());
			completedTasks.removeIf(task -> task.getUserId().equals(userId));

			return tasks;
		}
	}

	@Override
	public void taskComplete(DecomposedMarkdownDocumentProcessingTask decomposedMarkdownDocumentProcessingTask) {
		synchronized (inProgressTasks) {
			inProgressTasks.removeIf(task -> task == decomposedMarkdownDocumentProcessingTask);
			completedTasks.add(decomposedMarkdownDocumentProcessingTask);
		}
	}

	@Override
	public List<Document> listDocuments(UserId userId, ProjectId projectId) {
		return documentRepository.findAllByOwnerIdAndProjectId(userId, projectId).stream().map(doc -> doc.fromEntity())
				.toList();
	}

	@Override
	public List<DocumentSection> getSectionsBySequenceOrder(UserId userId, ProjectId projectId, String title,
			List<Integer> sequenceOrders) {

		MintyDoc doc = documentRepository.findByOwnerIdAndProjectIdAndTitleIgnoreCase(userId, projectId, title)
				.orElse(null);

		if (doc == null || !doc.getOwnerId().equals(userId)) {
			return null;
		}

		return documentSegmentRepository.findByDocument_IdAndSequenceOrderIn(doc.getId(), sequenceOrders).stream()
				.map(MintyDocSegment::fromEntityWithContent).toList();
	}

	@Override
	public Optional<Document> findByTitle(UserId userId, ProjectId projectId, String title) {
		Optional<MintyDoc> maybeDocument = documentRepository.findByTitleAndProjectId(title, projectId);
		if (maybeDocument.isEmpty()) {
			return Optional.empty();
		}

		MintyDoc document = maybeDocument.get();

		if (!document.getOwnerId().equals(userId)) {
			return Optional.empty();
		}

		return Optional.of(document.fromEntity());
	}

	@Override
	@Transactional
	public boolean deleteDocument(UserId userId, DocumentId documentId) {

		Document document = findByDocumentId(documentId);

		if (document == null) {
			logger.warn("Document " + documentId + " does not exist!");
			return false;
		}

		if (!document.ownerId().equals(userId)) {
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
	public void vectorizationComplete(UserId userId, DocumentId documentId, boolean success) {
		Optional<MintyDoc> maybeDoc = documentRepository.findById(documentId.value());
		if (maybeDoc.isEmpty()) {
			return;
		}
		MintyDoc doc = maybeDoc.get();
		if (doc.getOwnerId().equals(userId)) {
			doc.setVectorized(success);
			documentRepository.save(doc);
		}
	}

	@Override
	public boolean documentExists(Document document) {
		return documentRepository.existsByTitle(document.title());
	}

	@Override
	@Transactional
	public void addDocument(UserId userId, Document document) {

		MintyDoc mintyDoc = new MintyDoc();
		mintyDoc.setCreated(document.created());
		mintyDoc.setOwnerId(userId);
		mintyDoc.setProjectId(document.projectId());
		mintyDoc.setTitle(document.title());
		mintyDoc.setSummary(document.summary());
		mintyDoc.setUpdated(Instant.now());
		mintyDoc.setVectorized(true);

		List<MintyDocSegment> segments = new ArrayList<>();
		for (int i = 0; i < document.sections().size(); i++) {
			DocumentSection currentSection = document.sections().get(i);

			MintyDocSegment docSegment = new MintyDocSegment();
			docSegment.setContent(currentSection.content());
			docSegment.setDocument(mintyDoc);
			docSegment.setSequenceOrder(i);
			docSegment.setLevel(currentSection.level());
			docSegment.setParentIndex(currentSection.parentIndex());
			docSegment.setTitle(currentSection.title());
			segments.add(docSegment);
		}
		mintyDoc.setSegments(segments);

		documentRepository.save(mintyDoc);
	}

	@Override
	@Transactional
	public void updateDocument(UserId userId, Document document) throws NotFoundException, NotOwnedException {

		Optional<MintyDoc> maybeMintyDoc = documentRepository.findById(document.id().value());
		if (maybeMintyDoc.isEmpty()) {
			throw new NotFoundException("Document not found.");
		}

		MintyDoc mintyDoc = maybeMintyDoc.get();
		if (!mintyDoc.getOwnerId().equals(userId)) {
			throw new NotOwnedException("Not owned.");
		}

		mintyDoc.setTitle(document.title());
		mintyDoc.setUpdated(Instant.now());
		mintyDoc.setVectorized(true);

		List<MintyDocSegment> segments = new ArrayList<>();
		for (int i = 0; i < document.sections().size(); i++) {
			MintyDocSegment docSegment = new MintyDocSegment();
			docSegment.setContent(document.sections().get(i).content());
			docSegment.setDocument(mintyDoc);
			docSegment.setSequenceOrder(i);
			segments.add(docSegment);
		}
		mintyDoc.setSegments(segments);

		documentRepository.save(mintyDoc);
	}

	@Override
	public DocumentSection getDocumentSection(UserId userId, DocumentSectionId documentSectionId) {
		Optional<MintyDocSegment> maybeDocSegment = documentSegmentRepository.findById(documentSectionId.value());
		if (maybeDocSegment.isEmpty()) {
			return null;
		}
		MintyDocSegment docSegment = maybeDocSegment.get();

		return docSegment.fromEntityWithContent();
	}

	@Override
	public boolean documentOwnedBy(UserId userId, DocumentId documentId) {
		Optional<MintyDoc> doc = documentRepository.findById(documentId.value());
		return doc.isPresent() && userId.equals(doc.get().getOwnerId());
	}

	@Override
	public Document findByDocumentId(DocumentId documentId) {
		return documentRepository.findById(documentId.value()).map(doc -> {
			if (doc == null) {
				return null;
			}
			return doc.fromEntity();
		}).orElse(null);
	}

}
