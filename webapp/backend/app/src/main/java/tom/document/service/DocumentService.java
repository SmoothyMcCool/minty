package tom.document.service;

import java.io.File;
import java.util.List;

import tom.document.model.MintyDoc;

public interface DocumentService {

	void processFile(File file);

	void transformAndStore(File file, MintyDoc doc);

	void markDocumentComplete(MintyDoc doc);

	List<MintyDoc> listDocuments();

	boolean documentExists(String documentId);

	MintyDoc addDocument(int userId, MintyDoc document);

	boolean deleteDocument(int userId, String documentId);

	boolean documentOwnedBy(int userId, String documentId);

	MintyDoc findByDocumentId(String documentId);

}
