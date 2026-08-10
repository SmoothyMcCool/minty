package tom.analytics.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.LlmHourlyMetrics;
import tom.analytics.entity.LlmHourlyMetricsId;

public interface LlmHourlyMetricsRepository extends Repository<LlmHourlyMetrics, LlmHourlyMetricsId> {
	List<LlmHourlyMetrics> findByIdDayBetweenOrderByIdDayAscIdHourAsc(LocalDate from, LocalDate to);
}