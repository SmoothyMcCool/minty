package tom.task.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import tom.task.model.StandaloneTask;

@Service
public interface StandaloneTaskRepository extends CrudRepository<StandaloneTask, Integer> {

	public List<StandaloneTask> findAllByTriggeredTrue();

}
