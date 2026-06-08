package tom.document.service;

import java.util.List;

import tom.api.DocumentId;
import tom.api.UserId;
import tom.api.services.DocumentService;
import tom.document.service.tasks.DecomposedMarkdownDocumentProcessingTask;

public interface DocumentServiceInternal extends DocumentService {

	void taskComplete(DecomposedMarkdownDocumentProcessingTask decomposedMarkdownDocumentProcessingTask);

	List<String> getInProgressTaskNames(UserId userId);

	void vectorizationComplete(UserId userId, DocumentId documentId, boolean success);

	List<MintyDoc> listDocuments(UserId userId, ProjectId projectId);

}
