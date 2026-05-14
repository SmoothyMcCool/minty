package tom.meta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tom.meta.model.RequestStatus;
import tom.meta.model.RequestStatusEntity;

/**
 * Repository for the {@code request_status} lookup table.
 *
 * <p>
 * Records are seeded by the schema SQL. In normal operation the only method you
 * need is {@link #getByStatus(RequestStatus)}, called when transitioning an
 * {@link com.example.aimetrics.domain.AiRequest} to a new status.
 */
@Repository
public interface RequestStatusRepository extends JpaRepository<RequestStatusEntity, String> {

	/**
	 * Resolves the {@link RequestStatusEntity} for the given enum value. Throws
	 * {@link IllegalStateException} if the seed row is missing.
	 */
	default RequestStatusEntity getByStatus(RequestStatus status) {
		return findById(status.toDbValue()).orElseThrow(
				() -> new IllegalStateException("Missing request_status seed row for: " + status.toDbValue()));
	}
}