package tom.analytics.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.LlmDailyMetrics;

public interface LlmDailyMetricsRepository extends Repository<LlmDailyMetrics, LocalDate> {
	List<LlmDailyMetrics> findByDayBetweenOrderByDayAsc(LocalDate from, LocalDate to);
}