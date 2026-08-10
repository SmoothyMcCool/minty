package tom.analytics.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.UserLoginSummary;

public interface UserLoginSummaryRepository extends Repository<UserLoginSummary, UUID> {
	Optional<UserLoginSummary> findById(UUID userId);

	List<UserLoginSummary> findAll();
}