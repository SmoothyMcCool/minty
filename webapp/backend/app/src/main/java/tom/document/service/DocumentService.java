package tom.document.service;

import java.io.File;
import java.util.List;

import tom.api.DocumentId;
import tom.api.UserId;
import tom.document.model.MintyDoc;

public interface DocumentService {

	void processFile(File file);

	void transformAndStore(File file, MintyDoc doc);

	void markDocumentComplete(MintyDoc doc);

	List<MintyDoc> listDocuments();

	boolean documentExists(DocumentId documentId);

	MintyDoc addDocument(UserId userId, MintyDoc document);

	boolean deleteDocument(UserId userId, DocumentId documentId);

	boolean documentOwnedBy(UserId userId, DocumentId documentId);

	MintyDoc findByDocumentId(DocumentId documentId);

}
