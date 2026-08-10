package tom.assistant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.UserId;
import tom.assistant.model.Assistant;

public interface AssistantRepository extends JpaRepository<Assistant, UUID> {

	List<Assistant> findAllByOwnerId(UserId ownerId);

	Optional<Assistant> findByName(String name);

}
