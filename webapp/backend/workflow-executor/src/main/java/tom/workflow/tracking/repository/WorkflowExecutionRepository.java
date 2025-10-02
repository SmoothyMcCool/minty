package tom.workflow.tracking.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;
import tom.workflow.tracking.model.WorkflowExecution;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

	List<WorkflowExecution> findAllByOwnerId(UserId userId);

}
