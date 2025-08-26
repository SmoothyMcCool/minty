package tom.document.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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
	public void createLink(UUID assistantId, UUID documentId) {
		Assistant assistant = assistantRepository.getReferenceById(assistantId);
		MintyDoc document = documentRepository.getReferenceById(documentId);
		AssistantDocumentLink link = new AssistantDocumentLink(assistant, document);
		assistantDocumentLinkRepository.save(link);
	}

	@Override
	public void removeLink(UUID assistantId, UUID documentId) {
		assistantDocumentLinkRepository.deleteById(new AssistantDocumentId(assistantId, documentId));
	}

	@Override
	public void removeAllLinksToAssistant(UUID assistantId) {
		assistantDocumentLinkRepository.deleteAllByAssistantId(assistantId);
	}

	@Override
	public List<MintyDoc> getDocumentsForAssistant(UUID assistantId) {
		List<UUID> documentIds = assistantDocumentLinkRepository.findDocumentIdsByAssistantId(assistantId);
		return documentRepository.findAllById(documentIds);
	}

	@Override
	public List<UUID> getDocumentIdsForAssistant(UUID assistantId) {
		return assistantDocumentLinkRepository.findDocumentIdsByAssistantId(assistantId);
	}

	@Override
	public List<UUID> getAssistantIdsForDocument(UUID documentId) {
		return assistantDocumentLinkRepository.findAssistantIdsbyDocumentId(documentId);
	}

	@Override
	@Transactional
	public void updateLinksForAssistant(UUID assistantId, List<UUID> newDocumentIds) {
		List<UUID> currentDocIds = assistantDocumentLinkRepository.findDocumentIdsByAssistantId(assistantId);

		List<UUID> docsToRemove = currentDocIds.stream().filter(item -> !newDocumentIds.contains(item)).toList();
		List<UUID> docsToAdd = newDocumentIds.stream().filter(item -> !currentDocIds.contains(item)).toList();

		docsToRemove.forEach(documentId -> {
			removeLink(assistantId, documentId);
		});

		docsToAdd.forEach(documentId -> {
			createLink(assistantId, documentId);
		});

	}
}
