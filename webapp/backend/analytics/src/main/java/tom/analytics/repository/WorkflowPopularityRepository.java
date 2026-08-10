package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.WorkflowPopularity;

public interface WorkflowPopularityRepository extends Repository<WorkflowPopularity, UUID> {
	WorkflowPopularity findByWorkflowId(UUID workflowId);

	List<WorkflowPopularity> findAllByOrderByExecutionCountDesc();

	List<WorkflowPopularity> findAllByOrderByUniqueUsersDesc();

	List<WorkflowPopularity> findAllByOrderByCreationCountDesc();

	List<WorkflowPopularity> findAll();
}