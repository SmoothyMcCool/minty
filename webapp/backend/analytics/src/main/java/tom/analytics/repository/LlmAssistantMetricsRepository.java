package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.LlmAssistantMetrics;

public interface LlmAssistantMetricsRepository extends Repository<LlmAssistantMetrics, UUID> {
	LlmAssistantMetrics findById(UUID assistantId);

	List<LlmAssistantMetrics> findAllByOrderByRequestsDesc();

	List<LlmAssistantMetrics> findAll();
}