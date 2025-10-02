package tom.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;
import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.document.model.joins.AssistantDocumentId;
import tom.document.model.joins.AssistantDocumentLink;

public interface AssistantDocumentLinkRepository extends JpaRepository<AssistantDocumentLink, AssistantDocumentId> {

	@Transactional
	@Modifying
	@Query("delete from AssistantDocumentLink link where link.id.assistantId = :assistantId")
	void deleteAllByAssistantId(@Param("assistantId") AssistantId assistantId);

	@Query("select link.id.documentId from AssistantDocumentLink link where link.id.assistantId = :assistantId")
	List<DocumentId> findDocumentIdsByAssistantId(@Param("assistantId") AssistantId assistantId);

	@Query("select link.id.assistantId from AssistantDocumentLink link where link.id.documentId = :documentId")
	List<AssistantId> findAssistantIdsbyDocumentId(@Param("documentId") DocumentId documentId);

}
