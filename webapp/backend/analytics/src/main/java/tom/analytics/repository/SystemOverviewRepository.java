package tom.analytics.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.SystemOverview;

public interface SystemOverviewRepository extends Repository<SystemOverview, Integer> {
	Optional<SystemOverview> findFirstBy();
}