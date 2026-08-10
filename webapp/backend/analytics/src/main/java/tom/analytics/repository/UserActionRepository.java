package tom.analytics.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.analytics.entity.UserAction;
import tom.analytics.entity.UserActionType;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

	long countByUserIdAndActionType(UUID userId, UserActionType actionType);

	long countByUserIdAndActionTypeAndOccurredAtGreaterThanEqual(UUID userId, UserActionType actionType,
			LocalDateTime since);

	long countByActionType(UserActionType actionType);

	long countByActionTypeAndOccurredAtGreaterThanEqual(UserActionType actionType, LocalDateTime since);

	long countByAssistantIdAndActionType(UUID assistantId, UserActionType actionType);

	long countByWorkflowIdAndActionType(UUID workflowId, UserActionType actionType);

	List<UserAction> findByUserIdOrderByOccurredAtDesc(UUID userId);

	List<UserAction> findByUserIdAndActionTypeOrderByOccurredAtDesc(UUID userId, UserActionType actionType);

	List<UserAction> findByConversationIdOrderByOccurredAtAsc(UUID conversationId);

	List<UserAction> findByWorkflowIdOrderByOccurredAtDesc(UUID workflowId);

	List<UserAction> findByAssistantIdOrderByOccurredAtDesc(UUID assistantId);
}