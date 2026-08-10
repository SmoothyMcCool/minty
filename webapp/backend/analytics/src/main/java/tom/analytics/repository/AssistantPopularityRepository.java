package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.AssistantPopularity;

public interface AssistantPopularityRepository extends Repository<AssistantPopularity, UUID> {
	List<AssistantPopularity> findAllByOrderByConversationCountDesc();

	List<AssistantPopularity> findAllByOrderByMessageCountDesc();

	AssistantPopularity findById(UUID assistantId);

	List<AssistantPopularity> findAll();
}