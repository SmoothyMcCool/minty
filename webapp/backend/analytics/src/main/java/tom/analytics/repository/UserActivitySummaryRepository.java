package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.UserActivitySummary;

public interface UserActivitySummaryRepository extends Repository<UserActivitySummary, UUID> {
	UserActivitySummary findByUserId(UUID userId);

	List<UserActivitySummary> findAll();
}