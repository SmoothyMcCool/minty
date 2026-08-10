package tom.analytics.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.AssistantDailyUsage;
import tom.analytics.entity.AssistantDailyUsageId;

public interface AssistantDailyUsageRepository extends Repository<AssistantDailyUsage, AssistantDailyUsageId> {
	List<AssistantDailyUsage> findByIdDayBetween(LocalDate from, LocalDate to);

	List<AssistantDailyUsage> findByIdAssistantIdAndIdDayBetween(UUID assistantId, LocalDate from, LocalDate to);
}