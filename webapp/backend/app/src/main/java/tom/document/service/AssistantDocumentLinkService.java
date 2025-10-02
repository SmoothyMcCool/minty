package tom.document.service;

import java.util.List;

import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.document.model.MintyDoc;

public interface AssistantDocumentLinkService {

	void removeLink(AssistantId assistantId, DocumentId documentId);

	void removeAllLinksToAssistant(AssistantId assistantId);

	void createLink(AssistantId assistantId, DocumentId documentId);

	List<MintyDoc> getDocumentsForAssistant(AssistantId assistantId);

	List<DocumentId> getDocumentIdsForAssistant(AssistantId assistantId);

	List<AssistantId> getAssistantIdsForDocument(DocumentId documentId);

	void updateLinksForAssistant(AssistantId assistantId, List<DocumentId> newDocumentIds);
}
