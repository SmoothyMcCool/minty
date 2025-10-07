package tom.document.service;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.assistant.repository.Assistant;
import tom.assistant.repository.AssistantRepository;
import tom.document.model.MintyDoc;
import tom.document.model.joins.AssistantDocumentId;
import tom.document.model.joins.AssistantDocumentLink;
import tom.document.repository.AssistantDocumentLinkRepository;
import tom.document.repository.DocumentRepository;

@Service
public class AssistantDocumentLinkServiceImpl implements AssistantDocumentLinkService {

	private final DocumentRepository documentRepository;
	private final AssistantRepository assistantRepository;
	private final AssistantDocumentLinkRepository assistantDocumentLinkRepository;

	public AssistantDocumentLinkServiceImpl(DocumentRepository documentRepository,
			AssistantRepository assistantRepository, AssistantDocumentLinkRepository assistantDocumentLinkRepository) {

		this.documentRepository = documentRepository;
		this.assistantRepository = assistantRepository;
		this.assistantDocumentLinkRepository = assistantDocumentLinkRepository;
	}

	@Override
	@Transactional
	public void createLink(AssistantId assistantId, DocumentId documentId) {
		Assistant assistant = assistantRepository.getReferenceById(assistantId.value());
		MintyDoc document = documentRepository.getReferenceById(documentId.value());
		AssistantDocumentLink link = new AssistantDocumentLink(assistant, document);
		assistantDocumentLinkRepository.save(link);
	}

	@Override
	public void removeLink(AssistantId assistantId, DocumentId documentId) {
		assistantDocumentLinkRepository.deleteById(new AssistantDocumentId(assistantId.value(), documentId.value()));
	}

	@Override
	public void removeAllLinksToAssistant(AssistantId assistantId) {
		assistantDocumentLinkRepository.deleteAllByAssistantId(assistantId.value());
	}

	@Override
	public List<MintyDoc> getDocumentsForAssistant(AssistantId assistantId) {
		List<DocumentId> documentIds = assistantDocumentLinkRepository
				.findDocumentIdsByAssistantId(assistantId.value());
		return documentRepository.findAllById(documentIds.stream().map(item -> item.value()).toList());
	}

	@Override
	public List<DocumentId> getDocumentIdsForAssistant(AssistantId assistantId) {
		return assistantDocumentLinkRepository.findDocumentIdsByAssistantId(assistantId.value());
	}

	@Override
	public List<AssistantId> getAssistantIdsForDocument(DocumentId documentId) {
		return assistantDocumentLinkRepository.findAssistantIdsbyDocumentId(documentId.value()).stream()
				.map(item -> item).toList();
	}

	@Override
	@Transactional
	public void updateLinksForAssistant(AssistantId assistantId, List<DocumentId> newDocumentIds) {
		List<DocumentId> currentDocIds = assistantDocumentLinkRepository
				.findDocumentIdsByAssistantId(assistantId.value());

		List<DocumentId> docsToRemove = currentDocIds.stream().filter(item -> !newDocumentIds.contains(item)).toList();
		List<DocumentId> docsToAdd = newDocumentIds.stream().filter(item -> !currentDocIds.contains(item)).toList();

		docsToRemove.forEach(documentId -> {
			removeLink(assistantId, documentId);
		});

		docsToAdd.forEach(documentId -> {
			createLink(assistantId, documentId);
		});

	}
}
