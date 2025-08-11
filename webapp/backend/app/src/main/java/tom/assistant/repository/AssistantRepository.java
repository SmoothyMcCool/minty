package tom.assistant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface AssistantRepository extends JpaRepository<Assistant, Integer> {

	// All assistants owned by Owner ID or shared = true
	public List<Assistant> findAllByOwnerIdOrSharedTrue(Integer ownerId);
}
