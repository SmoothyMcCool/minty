package tom.document.service;

import java.io.File;
import java.util.List;
import java.util.UUID;

import tom.document.model.MintyDoc;

public interface DocumentService {

	void processFile(File file);

	void transformAndStore(File file, MintyDoc doc);

	void markDocumentComplete(MintyDoc doc);

	List<MintyDoc> listDocuments();

	boolean documentExists(UUID documentId);

	MintyDoc addDocument(UUID userId, MintyDoc document);

	boolean deleteDocument(UUID userId, UUID documentId);

	boolean documentOwnedBy(UUID userId, UUID documentId);

	MintyDoc findByDocumentId(UUID documentId);

}
