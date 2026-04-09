package tom.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.workflow.model.joins.UserWorkflowId;
import tom.workflow.model.joins.UserWorkflowLink;

public interface UserWorkflowLinkRepository extends JpaRepository<UserWorkflowLink, UserWorkflowId> {

	List<UserWorkflowLink> findById_UserId(UUID userId);

	List<UserWorkflowLink> findById_UserIdIn(List<UUID> userId);

	List<UserWorkflowLink> findById_WorkflowId(UUID workflowId);

	void deleteById_UserId(UUID userId);

	void deleteById_WorkflowId(UUID workflowId);

	Optional<UserWorkflowLink> findById_WorkflowIdAndId_UserId(UUID workflowId, UUID userId);

	Optional<UserWorkflowLink> findFirstById_WorkflowIdAndId_UserIdIn(UUID value, List<UUID> of);
}