package tom.task.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import tom.task.model.Task;

@Service
public interface TaskRepository extends CrudRepository<Task, Integer> {
}
