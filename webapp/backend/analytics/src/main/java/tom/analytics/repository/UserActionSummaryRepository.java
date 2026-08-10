package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.UserActionSummary;

public interface UserActionSummaryRepository extends Repository<UserActionSummary, UUID> {
	UserActionSummary findByUserId(UUID userId);

	List<UserActionSummary> findAll();
}