package tom.meta.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tom.meta.model.DailyMetrics;
import tom.meta.model.DailyMetrics.DailyMetricsId;

/**
 * Read-only repository for the {@code daily_metrics} view. Do not call
 * save/delete - the view is not writable.
 */
@Repository
public interface DailyMetricsRepository extends JpaRepository<DailyMetrics, DailyMetricsId> {

	@Query("SELECT d FROM DailyMetrics d WHERE d.id.day = :day ORDER BY d.totalRequests DESC")
	List<DailyMetrics> findByDay(@Param("day") LocalDate day);

	@Query("""
			SELECT d FROM DailyMetrics d
			 WHERE d.id.day BETWEEN :from AND :to
			 ORDER BY d.id.day DESC, d.totalRequests DESC
			""")
	List<DailyMetrics> findByDayBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

	default List<DailyMetrics> findToday() {
		return findByDay(LocalDate.now());
	}

	default List<DailyMetrics> findLastDays(int days) {
		LocalDate to = LocalDate.now();
		LocalDate from = to.minusDays(days - 1L);
		return findByDayBetween(from, to);
	}
}