package tom.workflow.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface WorkflowTaskRepository extends CrudRepository<WorkflowTask, Integer> {
}
