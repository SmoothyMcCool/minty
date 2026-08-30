package tom.conversation.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.assistant.Assistant;
import tom.api.model.conversation.ChatMessage;
import tom.api.model.conversation.Conversation;
import tom.api.model.conversation.MessageType;
import tom.api.services.assistant.AssistantManagementService;
import tom.assistant.service.management.AssistantManagementServiceInternal;
import tom.conversation.repository.ConversationRepository;
import tom.llm.service.LlmClientRegistry;

@Service
public class ConversationServiceImpl implements ConversationServiceInternal {

	private static final Logger logger = LogManager.getLogger(ConversationServiceImpl.class);

	private final AssistantManagementServiceInternal assistantManagementService;
	private final LlmClientRegistry llmClientRegistry;
	private final ConversationRepository conversationRepository;
	private final HashMap<ConversationId, Conversation> fakeConversationMap;

	public ConversationServiceImpl(ConversationRepository conversationRepository,
			AssistantManagementServiceInternal assistantManagementService, LlmClientRegistry llmClientRegistry) {
		this.assistantManagementService = assistantManagementService;
		this.llmClientRegistry = llmClientRegistry;
		this.conversationRepository = conversationRepository;
		fakeConversationMap = new HashMap<>();
	}

	@PostConstruct
	public void initialize() {
		assistantManagementService.setConversationService(this);
	}

	@Override
	public Conversation getConversation(UserId userId, ConversationId conversationId) {
		Optional<tom.conversation.model.Conversation> ce = conversationRepository.findById(conversationId.value());
		if (ce.isPresent()) {
			Conversation c = ce.get().fromEntity();
			if (c.getOwnerId().equals(userId)) {
				return c;
			}
		}
		return null;
	}

	@Override
	@Transactional
	public void deleteConversationsForAssistant(UserId userId, AssistantId assistantId) {
		List<tom.conversation.model.Conversation> conversations = conversationRepository
				.findAllByOwnerIdAndAssociatedAssistantId(userId, assistantId);

		SessionService sessionService = llmClientRegistry.getSessionService();

		conversations.forEach(conversation -> {
			String conversationKey = conversation.getId().toString();

			try {
				if (sessionService.findById(conversationKey) != null) {
					sessionService.delete(conversationKey);
				}
			} catch (Exception e) {
				logger.warn("Failed to delete session for conversation " + conversationKey, e);
			}
		});
	}

	@Override
	public AssistantId getAssistantIdFromConversationId(UserId userId, ConversationId conversationId) {
		tom.api.model.conversation.Conversation conversation;
		if (fakeConversationMap.containsKey(conversationId)) {
			conversation = fakeConversationMap.get(conversationId);
		} else {

			Optional<tom.conversation.model.Conversation> ce = conversationRepository.findById(conversationId.value());
			if (ce.isEmpty()) {
				return null;
			}
			conversation = ce.get().fromEntity();
		}

		AssistantId assistantId = conversation.getAssociatedAssistantId();
		if (assistantId == null) {
			return AssistantManagementService.DefaultAssistantId;
		}

		return assistantId;
	}

	@Override
	public List<ChatMessage> getChatMessages(UserId userId, ConversationId conversationId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			return List.of();
		}

		Optional<tom.conversation.model.Conversation> conversation = conversationRepository
				.findById(conversationId.value());
		if (conversation.isEmpty()) {
			return List.of();
		}

		SessionService sessionService = llmClientRegistry.getSessionService();
		String sessionKey = conversationId.value().toString();

		Session session = sessionService.findById(sessionKey);
		if (session == null) {
			return List.of();
		}

		EventFilter filter = EventFilter.builder().excludeArchived(true)
				.messageTypes(Set.of(org.springframework.ai.chat.messages.MessageType.USER,
						org.springframework.ai.chat.messages.MessageType.ASSISTANT,
						org.springframework.ai.chat.messages.MessageType.TOOL))
				.build();

		List<SessionEvent> events = sessionService.getEvents(sessionKey, filter);

		List<ChatMessage> result = toChatMessages(events.stream().map(SessionEvent::getMessage).toList());
		Collections.reverse(result);

		return result;
	}

	private static List<ChatMessage> toChatMessages(List<Message> messages) {
		Map<String, AssistantMessage.ToolCall> pendingCalls = new HashMap<>();
		List<ChatMessage> result = new ArrayList<>();

		for (Message message : messages) {
			switch (message.getMessageType()) {
			case USER -> result.add(new ChatMessage(MessageType.USER, message.getText()));

			case ASSISTANT -> {
				AssistantMessage assistantMessage = (AssistantMessage) message;
				if (assistantMessage.hasToolCalls()) {
					// Empty-text assistant message: it's a tool-call request, not a reply.
					// Stash each call; we'll emit it once we see the matching TOOL response.
					for (AssistantMessage.ToolCall call : assistantMessage.getToolCalls()) {
						pendingCalls.put(call.id(), call);
					}
				} else {
					result.add(new ChatMessage(MessageType.ASSISTANT, assistantMessage.getText()));
				}
			}

			case TOOL -> {
				if (message instanceof ToolResponseMessage toolResponseMessage) {
					for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
						AssistantMessage.ToolCall call = pendingCalls.remove(response.id());
						String arguments = call != null ? call.arguments() : "(unknown arguments)";
						String toolName = response.name() + "(" + arguments + ")";
						String toolResult = response.responseData();

						result.add(new ChatMessage(MessageType.TOOL, toolName + "\n\n" + toolResult));
					}
				} else {
					throw new IllegalStateException("Message type is TOOL but class is not ToolResponseMessage.");
				}
			}

			default -> throw new IllegalArgumentException("Unexpected value: " + message.getMessageType());
			}
		}

		return result;
	}

	private static MessageType toMessageType(org.springframework.ai.chat.messages.MessageType type) {
		return switch (type) {
		case USER -> MessageType.USER;
		case ASSISTANT -> MessageType.ASSISTANT;
		case TOOL -> MessageType.TOOL;
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	@Override
	@Transactional
	public boolean deleteConversation(UserId userId, ConversationId conversationId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			Conversation conversation = fakeConversationMap.get(conversationId);
			if (conversation != null && conversation.getOwnerId().equals(userId)) {
				fakeConversationMap.remove(conversationId);
				return true;
			} else {
				return false;
			}
		}

		Optional<tom.conversation.model.Conversation> conversation = conversationRepository
				.findById(conversationId.value());
		if (conversation.isPresent() && !conversation.get().getOwnerId().equals(userId)) {
			logger.warn("Conversation " + conversationId + " not owned by " + userId);
			return false;
		}

		SessionService sessionService = llmClientRegistry.getSessionService();
		sessionService.delete(conversationId.value().toString());

		conversationRepository.deleteById(conversationId.value());

		return true;
	}

	@Override
	@Transactional
	public boolean resetConversation(UserId userId, ConversationId conversationId) {
		Optional<tom.conversation.model.Conversation> conversation = conversationRepository
				.findById(conversationId.value());
		if (conversation.isPresent() && !conversation.get().getOwnerId().equals(userId)) {
			logger.warn("Conversation " + conversationId + " not owned by " + userId);
			return false;
		}

		SessionService sessionService = llmClientRegistry.getSessionService();
		String sessionKey = conversationId.value().toString();

		if (sessionService.findById(sessionKey) != null) {
			sessionService.delete(sessionKey);
		}

		return true;
	}

	@Override
	@Transactional
	public Conversation renameConversation(UserId userId, ConversationId conversationId, String title) {
		if (!conversationOwnedBy(userId, conversationId)) {
			return null;
		}

		Optional<tom.conversation.model.Conversation> conversation = conversationRepository
				.findById(conversationId.value());
		if (conversation.isEmpty()) {
			return null;
		}
		tom.conversation.model.Conversation ce = conversation.get();
		ce.setTitle(title);
		ce = conversationRepository.save(ce);

		return ce.fromEntity();
	}

	@Override
	@Transactional
	public Conversation associateProject(UserId userId, ConversationId conversationId, ProjectId projectId) {
		if (!conversationOwnedBy(userId, conversationId)) {
			return null;
		}

		Optional<tom.conversation.model.Conversation> conversation = conversationRepository
				.findById(conversationId.value());
		if (conversation.isEmpty()) {
			return null;
		}
		tom.conversation.model.Conversation ce = conversation.get();
		ce.setProjectId(projectId);
		ce = conversationRepository.save(ce);

		return ce.fromEntity();
	}

	@Override
	public boolean conversationOwnedBy(UserId userId, ConversationId conversationId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			Conversation conversation = fakeConversationMap.get(conversationId);
			return conversation.getOwnerId().equals(userId);
		}
		Optional<tom.conversation.model.Conversation> conversation = conversationRepository
				.findById(conversationId.value());
		if (conversation.isEmpty()) {
			return false;
		}
		return userId.equals(conversation.get().getOwnerId());
	}

	@Override
	@Transactional
	public Conversation newConversation(UserId userId, AssistantId assistantId) {
		return newConversation(userId, assistantId, null);
	}

	@Override
	@Transactional
	public Conversation newConversation(UserId userId, AssistantId assistantId, ProjectId projectId) {
		Assistant assistant = assistantManagementService.findAssistant(userId, assistantId);

		if (!assistant.hasMemory()) {
			ConversationId conversationId = new ConversationId(assistantId.value());
			boolean conversationExists = fakeConversationMap.containsKey(conversationId);

			if (conversationExists) {
				return fakeConversationMap.get(conversationId);
			}

			Conversation conversation = new Conversation();
			conversation.setOwnerId(userId);
			conversation.setAssociatedAssistantId(assistantId);
			conversation.setId(conversationId);
			conversation.setTitle(null);
			fakeConversationMap.put(conversationId, conversation);
			return conversation;
		}

		tom.conversation.model.Conversation conversation = new tom.conversation.model.Conversation();
		conversation.setAssociatedAssistantId(assistantId);
		conversation.setId(null);
		conversation.setProjectId(projectId);
		conversation.setOwnerId(userId);
		conversation.setTitle(null);

		return conversationRepository.save(conversation).fromEntity();
	}

	@Override
	public List<Conversation> listConversationsForUser(UserId userId) {
		List<Conversation> conversations = conversationRepository.findAllByOwnerId(userId).stream()
				.map(conversation -> conversation.fromEntity()).toList();

		// Remove all the internal workflow conversations.
		conversations = conversations.stream().filter(conversation -> !conversation.getAssociatedAssistantId()
				.equals(AssistantManagementService.DefaultAssistantId)).toList();

		return conversations;
	}

	@Override
	public List<Conversation> listConversationsForProject(UserId userId, ProjectId projectId) {
		return conversationRepository.findByProjectIdAndOwnerId(projectId, userId).stream()
				.map(conversation -> conversation.fromEntity()).toList();
	}

	@Override
	@Transactional
	public void updateLastUsed(ConversationId conversationId) {
		tom.conversation.model.Conversation conversation = conversationRepository.findById(conversationId.value())
				.orElse(null);
		if (conversation != null) {
			conversation.setLastUsed(Instant.now());
			conversationRepository.save(conversation);
		}
	}

}
