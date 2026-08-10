package tom.assistant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.assistant.model.joins.UserAssistantId;
import tom.assistant.model.joins.UserAssistantLink;

public interface UserAssistantLinkRepository extends JpaRepository<UserAssistantLink, UserAssistantId> {

	List<UserAssistantLink> findById_UserIdIn(List<UUID> userId);

	List<UserAssistantLink> findById_AssistantId(UUID assistantId);

	Optional<UserAssistantLink> findFirstById_AssistantIdAndId_UserIdIn(UUID assistantId, List<UUID> userIds);

	Optional<UserAssistantLink> findById_AssistantIdAndId_UserId(UUID assistantId, UUID userIds);

	void deleteById_UserId(UUID userId);

	void deleteById_AssistantId(UUID assistantId);
}