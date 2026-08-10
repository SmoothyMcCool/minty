package tom.analytics.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.AssistantLeaderboard;

public interface AssistantLeaderboardRepository extends Repository<AssistantLeaderboard, Long> {
	List<AssistantLeaderboard> findAllByOrderByRankingAsc();
}