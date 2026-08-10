package tom.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import tom.analytics.entity.UserWorkflowSummary;
import tom.analytics.entity.UserWorkflowSummaryId;

public interface UserWorkflowSummaryRepository extends Repository<UserWorkflowSummary, UserWorkflowSummaryId> {
	List<UserWorkflowSummary> findByIdUserId(UUID userId);

	List<UserWorkflowSummary> findByIdUserIdOrderByExecutionCountDesc(UUID userId);

	List<UserWorkflowSummary> findByIdWorkflowId(UUID workflowId);

	List<UserWorkflowSummary> findAllByOrderByExecutionCountDesc();
}