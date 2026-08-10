package tom.analytics.repository;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.LlmSystemOverview;

public interface LlmSystemOverviewRepository extends Repository<LlmSystemOverview, Integer> {
	LlmSystemOverview findFirstBy();
}