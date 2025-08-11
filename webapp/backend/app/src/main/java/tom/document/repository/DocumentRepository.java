package tom.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.document.model.MintyDoc;

@Service
public interface DocumentRepository extends JpaRepository<MintyDoc, String> {

	void deleteByDocumentId(String documentId);

	boolean existsByDocumentId(String documentId);

	MintyDoc findByDocumentId(String documentId);

}
