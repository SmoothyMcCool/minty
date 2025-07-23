package tom.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface WorkflowRepository extends JpaRepository<Workflow, Integer> {

	// All assistants owned by Owner ID or shared = true
	public List<Workflow> findAllByOwnerIdOrSharedTrue(Integer ownerId);
}
