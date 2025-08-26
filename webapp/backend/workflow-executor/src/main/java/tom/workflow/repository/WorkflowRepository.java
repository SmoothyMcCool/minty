package tom.workflow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

	// All assistants owned by Owner ID or shared = true
	List<Workflow> findAllByOwnerIdOrSharedTrue(UUID ownerId);

	List<Workflow> findAllByTriggeredTrue();
}
