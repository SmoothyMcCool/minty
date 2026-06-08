package tom.api.services;

import java.io.File;
import java.util.List;
import java.util.Optional;

import tom.api.DocumentId;
import tom.api.DocumentSectionId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSection;
import tom.api.model.document.SpreadsheetFormat;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;

public interface DocumentService {

	String fileToMarkdown(File file, SpreadsheetFormat tsv);

	void fileToMarkdownAndDecompose(UserId userId, ProjectId projectId, File file, boolean summarize) throws Exception;

	boolean documentExists(Document document);

	void addDocument(UserId userId, Document document);

	void updateDocument(UserId userId, Document document) throws NotFoundException, NotOwnedException;

	boolean deleteDocument(UserId userId, DocumentId documentId);

	boolean documentOwnedBy(UserId userId, DocumentId documentId);

	Document findByDocumentId(DocumentId documentId);

	List<Document> listDocuments(UserId userId, ProjectId projectId);

	DocumentSection getDocumentSection(UserId userId, DocumentSectionId documentSectionId);

	List<DocumentSection> getSectionsBySequenceOrder(UserId userId, ProjectId projectId, String title,
			List<Integer> sectionIndices);

	Optional<Document> findByTitle(UserId userId, ProjectId projectId, String title);

}
