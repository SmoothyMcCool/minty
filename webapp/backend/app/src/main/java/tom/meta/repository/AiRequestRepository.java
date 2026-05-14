package tom.meta.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tom.meta.model.AiRequest;
import tom.meta.model.RequestStatusEntity;

/**
 * Repository for the {@code requests} table.
 *
 * <p>
 * Lifecycle update methods use targeted {@code @Modifying} JPQL updates so that
 * only the relevant columns are written. Each must be called inside a
 * transaction.
 */
@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {

	// ----------------------------------------------------------------
	// Lookup helpers
	// ----------------------------------------------------------------

	List<AiRequest> findByUserId(UUID userId);

	Page<AiRequest> findByUserId(UUID userId, Pageable pageable);

	List<AiRequest> findByConversationId(UUID conversationId);

	List<AiRequest> findByUserIdAndConversationIdAndCompletedAtIsNull(UUID userId, UUID conversationId);

	/** Finds all requests currently in the given status, oldest first. */
	@Query("""
			SELECT r
			  FROM AiRequest r
			 WHERE r.status.status = :status
			 ORDER BY r.queuedAt ASC
			""")
	List<AiRequest> findByStatusOrderByQueuedAt(@Param("status") String status);

	Optional<AiRequest> findById(UUID llmRequestId);

	// ----------------------------------------------------------------
	// Lifecycle update methods
	// ----------------------------------------------------------------

	/**
	 * Marks the request as PROCESSING and records {@code dequeued_at}.
	 */
	@Modifying
	@Query("""
			UPDATE AiRequest r
			   SET r.status.status = 'processing',
			       r.dequeuedAt = :dequeuedAt
			 WHERE r.id = :id
			""")
	int markDequeued(@Param("id") UUID id, @Param("dequeuedAt") Instant dequeuedAt);

	/**
	 * Records the timestamp of the very first streamed token.
	 */
	@Modifying
	@Query("""
			UPDATE AiRequest r
			   SET r.firstTokenAt = :firstTokenAt
			 WHERE r.id = :id
			""")
	int recordFirstToken(@Param("id") UUID id, @Param("firstTokenAt") Instant firstTokenAt);

	/**
	 * Marks the request COMPLETED and records {@code completed_at}.
	 */
	@Modifying
	@Query("""
			UPDATE AiRequest r
			   SET r.status.status = 'completed',
			       r.completedAt = :completedAt
			 WHERE r.id = :id
			""")
	int markCompleted(@Param("id") UUID id, @Param("completedAt") Instant completedAt);

	/**
	 * Marks the request FAILED and stores the error message.
	 */
	@Modifying
	@Query("""
			UPDATE AiRequest r
			   SET r.status.status = 'failed',
			       r.completedAt = :failedAt,
			       r.error = :error
			 WHERE r.id = :id
			""")
	int markFailed(@Param("id") UUID id, @Param("failedAt") Instant failedAt, @Param("error") String error);

	// ----------------------------------------------------------------
	// Reporting helpers
	// ----------------------------------------------------------------

	/** Counts requests within a time window. */
	@Query("""
			SELECT COUNT(r) AS cnt
			  FROM AiRequest r
			 WHERE r.createdAt BETWEEN :from AND :to
			 ORDER BY cnt DESC
			""")
	List<Object[]> countByModelBetween(@Param("from") Instant from, @Param("to") Instant to);

	/**
	 * Returns completed requests that are missing a first_token_at.
	 */
	@Query("""
			SELECT r
			  FROM AiRequest r
			 WHERE r.status.status = 'completed'
			   AND r.firstTokenAt IS NULL
			""")
	List<AiRequest> findCompletedWithoutFirstToken();

	List<AiRequest> findByStatusIn(List<RequestStatusEntity> of);

}