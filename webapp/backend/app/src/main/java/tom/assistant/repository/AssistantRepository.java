package tom.assistant.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public interface AssistantRepository extends CrudRepository<Assistant, Integer> {

    // All assistants owned by Owner ID or shared = true
    public List<Assistant> findAllByOwnerIdOrSharedTrue(Integer ownerId);
}
