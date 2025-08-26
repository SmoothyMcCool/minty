package tom.document.service;

import java.util.List;
import java.util.UUID;

import tom.document.model.MintyDoc;

public interface AssistantDocumentLinkService {

	void removeLink(UUID assistantId, UUID documentId);

	void removeAllLinksToAssistant(UUID assistantId);

	void createLink(UUID assistantId, UUID documentId);

	List<MintyDoc> getDocumentsForAssistant(UUID assistantId);

	List<UUID> getDocumentIdsForAssistant(UUID assistantId);

	List<UUID> getAssistantIdsForDocument(UUID documentId);

	void updateLinksForAssistant(UUID assistantId, List<UUID> newDocumentIds);
}
