package tom.document.service;

import java.io.File;
import java.util.List;

import tom.api.DocumentId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.services.ProjectService;
import tom.api.services.document.DocumentService;
import tom.document.model.MintyDoc;
import tom.document.service.tasks.DecomposedMarkdownDocumentProcessingTask;

public interface DocumentServiceInternal extends DocumentService {

	void processFile(File file);

	void processFileToMarkdownAndDecompose(UserId id, ProjectId projectId, File file, ProjectService projectService,
			boolean summarize) throws Exception;

	void transformAndStore(File file, MintyDoc doc);

	void markDocumentComplete(MintyDoc doc);

	void markDocumentFailed(MintyDoc doc);

	List<MintyDoc> listDocuments(UserId userId);

	boolean documentExists(MintyDoc document);

	MintyDoc addDocument(UserId userId, MintyDoc document);

	boolean deleteDocument(UserId userId, DocumentId documentId);

	boolean documentOwnedBy(UserId userId, DocumentId documentId);

	MintyDoc findByDocumentId(DocumentId documentId);

	List<String> getInProgressTaskNames(UserId userId);

	void taskComplete(DecomposedMarkdownDocumentProcessingTask decomposedMarkdownDocumentProcessingTask);

}
