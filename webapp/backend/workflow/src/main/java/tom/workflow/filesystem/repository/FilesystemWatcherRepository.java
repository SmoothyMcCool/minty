package tom.workflow.filesystem.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface FilesystemWatcherRepository extends CrudRepository<FilesystemWatcher, Integer> {
}
