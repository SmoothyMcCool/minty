package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import tom.analytics.entity.ConversationStatistics;

@Repository
public interface ConversationStatisticsRepository extends AnalyticsRepository<ConversationStatistics, UUID> {
	List<ConversationStatistics> findByUserId(UUID userId);

	List<ConversationStatistics> findByAssistantId(UUID assistantId);

	List<ConversationStatistics> findByUserIdAndAssistantId(UUID userId, UUID assistantId);
}