package tom.document.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.api.DocumentId;
import tom.document.model.MintyDoc;

@Service
public interface DocumentRepository extends JpaRepository<MintyDoc, UUID> {

	void deleteByDocumentId(DocumentId documentId);

	boolean existsByDocumentId(DocumentId documentId);

	MintyDoc findByDocumentId(DocumentId documentId);

}
