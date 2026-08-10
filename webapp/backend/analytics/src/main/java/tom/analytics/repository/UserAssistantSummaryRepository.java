package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.UserAssistantSummary;
import tom.analytics.entity.UserAssistantSummaryId;

public interface UserAssistantSummaryRepository extends Repository<UserAssistantSummary, UserAssistantSummaryId> {
	List<UserAssistantSummary> findByIdUserId(UUID userId);

	List<UserAssistantSummary> findByIdAssistantId(UUID assistantId);

	List<UserAssistantSummary> findByIdUserIdAndIdAssistantId(UUID userId, UUID assistantId);

	List<UserAssistantSummary> findAll();
}
