package tom.meta.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tom.meta.model.RequestMetrics;

/**
 * Repository for the {@code request_metrics} table.
 *
 * <p>
 * The computed duration columns are written by the MariaDB trigger
 * {@code trg_compute_metrics} on completion. Token counts are updated
 * separately via {@link #updateTokenCounts} once the upstream response
 * finishes.
 */
@Repository
public interface RequestMetricsRepository extends JpaRepository<RequestMetrics, UUID> {

	/**
	 * Stores token counts after the upstream API call completes. Duration columns
	 * are already set by the DB trigger at this point.
	 */
	@Modifying
	@Query("""
			UPDATE RequestMetrics m
			   SET m.promptTokens     = :promptTokens,
			       m.completionTokens = :completionTokens
			 WHERE m.id = :id
			""")
	int updateTokenCounts(@Param("id") UUID id, @Param("promptTokens") int promptTokens,
			@Param("completionTokens") int completionTokens);

}
