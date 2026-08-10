package tom.analytics.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.DailySystemStatistics;

public interface DailySystemStatisticsRepository extends Repository<DailySystemStatistics, LocalDate> {

	List<DailySystemStatistics> findByDayBetweenOrderByDayAsc(LocalDate from, LocalDate to);

	DailySystemStatistics findByDay(LocalDate day);

	List<DailySystemStatistics> findAllByOrderByDayAsc();
}