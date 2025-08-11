package tom.document.service;

import java.util.List;

import tom.document.model.MintyDoc;

public interface AssistantDocumentLinkService {

	void removeLink(Integer assistantId, String documentId);

	void removeAllLinksToAssistant(Integer assistantId);

	void createLink(Integer assistantId, String documentId);

	List<MintyDoc> getDocumentsForAssistant(Integer assistantId);

	List<String> getDocumentIdsForAssistant(Integer assistantId);

	List<Integer> getAssistantIdsForDocument(String documentId);

	void updateLinksForAssistant(Integer assistantId, List<String> newDocumentIds);
}
