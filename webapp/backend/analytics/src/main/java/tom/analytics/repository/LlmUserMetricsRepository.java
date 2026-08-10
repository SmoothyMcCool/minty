package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.LlmUserMetrics;

public interface LlmUserMetricsRepository extends Repository<LlmUserMetrics, UUID> {
	LlmUserMetrics findById(UUID userId);

	List<LlmUserMetrics> findAllByOrderByRequestsDesc();

	List<LlmUserMetrics> findAll();
}