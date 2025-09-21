package tom.conversation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import tom.api.services.assistant.AssistantManagementService;
import tom.assistant.service.management.AssistantManagementServiceInternal;
import tom.conversation.model.Conversation;
import tom.conversation.model.ConversationEntity;
import tom.conversation.repository.ConversationRepository;
import tom.model.Assistant;
import tom.model.ChatMessage;
import tom.ollama.service.OllamaService;

@Service
public class ConversationServiceImpl implements ConversationServiceInternal {

	private static final Logger logger = LogManager.getLogger(ConversationServiceImpl.class);

	private final AssistantManagementServiceInternal assistantManagementService;
	private final OllamaService ollamaService;
	private final ConversationRepository conversationRepository;
	private final HashMap<UUID, Conversation> fakeConversationMap;

	public ConversationServiceImpl(ConversationRepository conversationRepository,
			AssistantManagementServiceInternal assistantManagementService, OllamaService ollamaService) {
		this.assistantManagementService = assistantManagementService;
		this.ollamaService = ollamaService;
		this.conversationRepository = conversationRepository;
		fakeConversationMap = new HashMap<>();
	}

	@PostConstruct
	public void initialize() {
		assistantManagementService.setConversationService(this);
	}

	@Override
	@Transactional
	public void deleteConversationsForAssistant(UUID userId, UUID assistantId) {
		List<ConversationEntity> conversations = conversationRepository.findAllByOwnerIdAndAssociatedAssistantId(userId,
				assistantId);
		ChatMemoryRepository chatMemoryRepository = ollamaService.getChatMemoryRepository();

		conversations.forEach(conversation -> {
			chatMemoryRepository.deleteByConversationId(conversation.getConversationId().toString());
		});

	}

	@Override
	public UUID getAssistantIdFromConversationId(UUID userId, UUID conversationId) {
		Conversation conversation;
		if (fakeConversationMap.containsKey(conversationId)) {
			conversation = fakeConversationMap.get(conversationId);
		} else {

			ConversationEntity ce = conversationRepository.findByConversationId(conversationId);
			if (ce == null) {
				return null;
			}
			conversation = conversationRepository.findByConversationId(conversationId).fromEntity();
		}

		UUID assistantId = conversation.getAssociatedAssistantId();
		if (assistantId == null) {
			return AssistantManagementService.DefaultAssistantId;
		}

		return assistantId;
	}

	@Override
	public List<ChatMessage> getChatMessages(UUID userId, UUID conversationId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			return List.of();
		}

		List<ChatMessage> result = new ArrayList<>();
		ChatMemory chatMemory = ollamaService.getChatMemory();

		List<Message> messages = chatMemory.get(conversationId.toString());
		result = messages.stream()
				.map(message -> new ChatMessage(message.getMessageType() == MessageType.USER, message.getText()))
				.collect(Collectors.toList());
		Collections.reverse(result);

		return result;
	}

	@Override
	@Transactional
	public boolean deleteConversation(UUID userId, UUID conversationId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			Conversation conversation = fakeConversationMap.get(conversationId);
			if (conversation.getOwnerId().equals(userId)) {
				fakeConversationMap.remove(conversationId);
				return true;
			} else {
				return false;
			}
		}

		ConversationEntity conversation = conversationRepository.findByConversationId(conversationId);
		if (!conversation.getOwnerId().equals(userId)) {
			logger.warn("Conversation " + conversationId + " not owned by " + userId);
			return false;
		}

		ChatMemory chatMemory = ollamaService.getChatMemory();

		chatMemory.clear(conversationId.toString());
		conversationRepository.deleteByConversationId(conversationId);

		return true;
	}

	@Override
	public boolean conversationOwnedBy(UUID conversationId, UUID userId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			Conversation conversation = fakeConversationMap.get(conversationId);
			return conversation.getOwnerId().equals(userId);
		}
		ConversationEntity conversation = conversationRepository.findByConversationId(conversationId);
		if (conversation == null) {
			return false;
		}
		return userId.equals(conversation.getOwnerId());
	}

	@Override
	@Transactional
	public Conversation newConversation(UUID userId, UUID assistantId) {
		Assistant assistant = assistantManagementService.findAssistant(userId, assistantId);

		if (!assistant.hasMemory()) {
			boolean conversationExists = fakeConversationMap.containsKey(assistantId);

			if (conversationExists) {
				return fakeConversationMap.get(assistantId);
			}

			Conversation conversation = new Conversation();
			conversation.setOwnerId(userId);
			conversation.setAssociatedAssistantId(assistantId);
			conversation.setConversationId(assistantId);
			conversation.setTitle(null);
			fakeConversationMap.put(assistantId, conversation);
			return conversation;
		}

		ConversationEntity conversation = new ConversationEntity();
		conversation.setAssociatedAssistantId(assistantId);
		conversation.setConversationId(null);
		conversation.setOwnerId(userId);
		conversation.setTitle(null);

		return conversationRepository.save(conversation).fromEntity();
	}

	@Override
	public List<Conversation> listConversationsForUser(UUID userId) {
		List<Conversation> conversations = conversationRepository.findAllByOwnerId(userId).stream()
				.map(conversation -> conversation.fromEntity()).toList();

		// Remove all the internal workflow conversations.
		conversations = conversations.stream().filter(conversation -> !conversation.getAssociatedAssistantId()
				.equals(AssistantManagementService.DefaultAssistantId)).toList();

		return conversations;
	}

}
