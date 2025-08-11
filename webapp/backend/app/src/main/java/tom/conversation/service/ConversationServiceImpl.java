package tom.conversation.service;

import java.util.ArrayList;
import java.util.Collections;
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
import tom.assistant.service.management.AssistantManagementServiceInternal;
import tom.conversation.model.Conversation;
import tom.conversation.repository.ConversationRepository;
import tom.model.Assistant;
import tom.model.ChatMessage;
import tom.ollama.service.OllamaService;
import tom.task.services.assistant.AssistantManagementService;
import tom.user.service.UserServiceInternal;

@Service
public class ConversationServiceImpl implements ConversationServiceInternal {

	private static final Logger logger = LogManager.getLogger(ConversationServiceImpl.class);

	private final AssistantManagementServiceInternal assistantManagementService;
	private final UserServiceInternal userService;
	private final OllamaService ollamaService;
	private final ConversationRepository conversationRepository;

	public ConversationServiceImpl(ConversationRepository conversationRepository, UserServiceInternal userService,
			AssistantManagementServiceInternal assistantManagementService, OllamaService ollamaService) {
		this.userService = userService;
		this.assistantManagementService = assistantManagementService;
		this.ollamaService = ollamaService;
		this.conversationRepository = conversationRepository;
	}

	@PostConstruct
	public void initialize() {
		assistantManagementService.setConversationService(this);
	}

	@Override
	public void deleteConversationsForAssistant(int userId, int assistantId) {
		List<Conversation> conversations = conversationRepository.findAllByOwnerIdAndAssociatedAssistantId(userId,
				assistantId);
		ChatMemoryRepository chatMemoryRepository = ollamaService.getChatMemoryRepository();

		conversations.forEach(conversation -> {
			chatMemoryRepository.deleteByConversationId(conversation.getConversationId());
		});

	}

	@Override
	public String getUserNameFromConversationId(String conversationId) {
		Conversation conversation = conversationRepository.findByConversationId(conversationId);
		return userService.getUsernameFromId(conversation.getOwnerId());
	}

	@Override
	public int getAssistantIdFromConversationId(String conversationId) {
		Conversation conversation = conversationRepository.findByConversationId(conversationId);
		Integer assistantId = conversation.getAssociatedAssistantId();
		if (assistantId == null) {
			return AssistantManagementService.DefaultAssistantId;
		}
		return assistantId;
	}

	@Override
	public List<ChatMessage> getChatMessages(int userId, String conversationId) {
		String model = getModelforConversation(userId, conversationId);

		List<ChatMessage> result = new ArrayList<>();

		if (model != null) {
			ChatMemory chatMemory = ollamaService.getChatMemory();

			List<Message> messages = chatMemory.get(conversationId);
			result = messages.stream()
					.map(message -> new ChatMessage(message.getMessageType() == MessageType.USER, message.getText()))
					.collect(Collectors.toList());
			Collections.reverse(result);
		}

		return result;
	}

	@Override
	public List<List<ChatMessage>> getChatMessagesForWorkflow(String workflowName) {

		List<Conversation> conversations = conversationRepository.findAllByAssociatedWorkflow(workflowName);

		ChatMemoryRepository chatMemoryRepository = ollamaService.getChatMemoryRepository();

		List<List<ChatMessage>> chatMessages = conversations.stream().map(conversation -> {
			List<ChatMessage> msgs = chatMemoryRepository.findByConversationId(conversation.getConversationId())
					.stream().map(message -> {
						return new ChatMessage(message.getMessageType().compareTo(MessageType.USER) == 0,
								message.getText());
					}).toList();
			return msgs;
		}).toList();

		return chatMessages;
	}

	@Override
	public void deleteConversationsForWorkflow(int userId, String workflowName) {

		List<Conversation> conversations = conversationRepository.findAllByAssociatedWorkflow(workflowName);

		ChatMemoryRepository chatMemoryRepository = ollamaService.getChatMemoryRepository();

		conversations.forEach(conversation -> {
			logger.info("Deleting workflow conversation " + conversation.getConversationId());
			chatMemoryRepository.deleteByConversationId(conversation.getConversationId());
		});
		return;
	}

	@Override
	@Transactional
	public boolean deleteConversation(int userId, String conversationId) {
		Conversation conversation = conversationRepository.findByConversationId(conversationId);
		if (conversation.getOwnerId() != userId) {
			logger.warn("Conversation " + conversationId + " not owned by " + userId);
			return false;
		}

		ChatMemory chatMemory = ollamaService.getChatMemory();

		chatMemory.clear(conversationId);
		conversationRepository.deleteByConversationId(conversationId);

		return true;
	}

	@Override
	public boolean conversationOwnedBy(String conversationId, int userId) {
		Conversation conversation = conversationRepository.findByConversationId(conversationId);
		if (conversation == null) {
			return false;
		}
		return userId == conversation.getOwnerId();
	}

	@Override
	public Conversation newConversation(int userId, int assistantId) {
		Conversation conversation = new Conversation();
		conversation.setAssociatedAssistantId(assistantId);
		conversation.setAssociatedWorkflow(null);
		conversation.setConversationId(UUID.randomUUID().toString());
		conversation.setId(null);
		conversation.setOwnerId(userId);
		conversation.setTitle(null);

		return conversationRepository.save(conversation);
	}

	@Override
	public Conversation newConversationForWorkflow(int userId, int assistantId, String workflowName) {
		Conversation conversation = new Conversation();
		conversation.setAssociatedAssistantId(assistantId);
		conversation.setAssociatedWorkflow(workflowName);
		conversation.setConversationId(UUID.randomUUID().toString());
		conversation.setId(null);
		conversation.setOwnerId(userId);
		conversation.setTitle(null);

		return conversationRepository.save(conversation);
	}

	@Override
	public List<Conversation> listConversationsForUser(int userId) {
		return conversationRepository.findAllByOwnerId(userId);
	}

	private String getModelforConversation(int userId, String conversationId) {
		int assistantId = getAssistantIdFromConversationId(conversationId);

		Assistant assistant = assistantManagementService.findAssistant(userId, assistantId);
		if (assistant == null) {
			return "";
		}

		return assistant.model();
	}
}
