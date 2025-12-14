package tom.conversation.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.conversation.model.Conversation;
import tom.api.model.Assistant;
import tom.api.model.ChatMessage;
import tom.api.services.assistant.AssistantManagementService;
import tom.assistant.service.management.AssistantManagementServiceInternal;
import tom.conversation.model.ConversationEntity;
import tom.conversation.repository.ConversationRepository;
import tom.ollama.service.OllamaService;

@Service
public class ConversationServiceImpl implements ConversationServiceInternal {

	private static final Logger logger = LogManager.getLogger(ConversationServiceImpl.class);

	private final AssistantManagementServiceInternal assistantManagementService;
	private final OllamaService ollamaService;
	private final ConversationRepository conversationRepository;
	private final HashMap<ConversationId, Conversation> fakeConversationMap;

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
	public Conversation getConversation(UserId userId, ConversationId conversationId) {
		Optional<ConversationEntity> ce = conversationRepository.findById(conversationId.value());
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
		List<ConversationEntity> conversations = conversationRepository.findAllByOwnerIdAndAssociatedAssistantId(userId,
				assistantId);
		ChatMemoryRepository chatMemoryRepository = ollamaService.getChatMemoryRepository();

		conversations.forEach(conversation -> {
			chatMemoryRepository.deleteByConversationId(conversation.getConversationId().toString());
		});

	}

	@Override
	public AssistantId getAssistantIdFromConversationId(UserId userId, ConversationId conversationId) {
		Conversation conversation;
		if (fakeConversationMap.containsKey(conversationId)) {
			conversation = fakeConversationMap.get(conversationId);
		} else {

			Optional<ConversationEntity> ce = conversationRepository.findById(conversationId.value());
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

		List<ChatMessage> result = new ArrayList<>();
		ChatMemory chatMemory = ollamaService.getChatMemory();

		List<Message> messages = chatMemory.get(conversationId.value().toString());
		result = messages.stream()
				.map(message -> new ChatMessage(message.getMessageType() == MessageType.USER, message.getText()))
				.collect(Collectors.toList());
		Collections.reverse(result);

		return result;
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

		Optional<ConversationEntity> conversation = conversationRepository.findById(conversationId.value());
		if (conversation.isPresent() && !conversation.get().getOwnerId().equals(userId)) {
			logger.warn("Conversation " + conversationId + " not owned by " + userId);
			return false;
		}

		ChatMemory chatMemory = ollamaService.getChatMemory();

		chatMemory.clear(conversationId.value().toString());
		conversationRepository.deleteById(conversationId.value());

		return true;
	}

	@Override
	@Transactional
	public boolean resetConversation(UserId userId, ConversationId conversationId) {
		Optional<ConversationEntity> conversation = conversationRepository.findById(conversationId.value());
		if (conversation.isPresent() && !conversation.get().getOwnerId().equals(userId)) {
			logger.warn("Conversation " + conversationId + " not owned by " + userId);
			return false;
		}

		ChatMemory chatMemory = ollamaService.getChatMemory();

		chatMemory.clear(conversationId.value().toString());

		return true;
	}

	@Override
	@Transactional
	public Conversation renameConversation(UserId userId, ConversationId conversationId, String title) {
		if (!conversationOwnedBy(userId, conversationId)) {
			return null;
		}

		Optional<ConversationEntity> conversation = conversationRepository.findById(conversationId.value());
		if (conversation.isEmpty()) {
			return null;
		}
		ConversationEntity ce = conversation.get();
		ce.setTitle(title);
		ce = conversationRepository.save(ce);

		return ce.fromEntity();
	}

	@Override
	public boolean conversationOwnedBy(UserId userId, ConversationId conversationId) {

		if (fakeConversationMap.containsKey(conversationId)) {
			Conversation conversation = fakeConversationMap.get(conversationId);
			return conversation.getOwnerId().equals(userId);
		}
		Optional<ConversationEntity> conversation = conversationRepository.findById(conversationId.value());
		if (conversation.isEmpty()) {
			return false;
		}
		return userId.equals(conversation.get().getOwnerId());
	}

	@Override
	@Transactional
	public Conversation newConversation(UserId userId, AssistantId assistantId) {
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
			conversation.setConversationId(conversationId);
			conversation.setTitle(null);
			fakeConversationMap.put(conversationId, conversation);
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
	public List<Conversation> listConversationsForUser(UserId userId) {
		List<Conversation> conversations = conversationRepository.findAllByOwnerId(userId).stream()
				.map(conversation -> conversation.fromEntity()).toList();

		// Remove all the internal workflow conversations.
		conversations = conversations.stream().filter(conversation -> !conversation.getAssociatedAssistantId()
				.equals(AssistantManagementService.DefaultAssistantId)).toList();

		return conversations;
	}

	@Override
	@Transactional
	public void updateLastUsed(ConversationId conversationId) {
		ConversationEntity conversation = conversationRepository.findById(conversationId.value()).orElse(null);
		if (conversation != null) {
			conversation.setLastUsed(Instant.now());
			conversationRepository.save(conversation);
		}
	}

}
