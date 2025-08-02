package tom.conversation.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.assistant.service.AssistantServiceInternal;
import tom.model.Assistant;
import tom.ollama.service.MintyOllamaModel;
import tom.ollama.service.OllamaService;
import tom.user.service.UserServiceInternal;

@Service
public class ConversationServiceImpl implements ConversationServiceInternal {

	private static final Logger logger = LogManager.getLogger(ConversationServiceImpl.class);

	private final AssistantServiceInternal assistantService;
	private final UserServiceInternal userService;
	private final OllamaService ollamaService;

	public ConversationServiceImpl(UserServiceInternal userService, AssistantServiceInternal assistantService,
			OllamaService ollamaService) {
		this.userService = userService;
		this.assistantService = assistantService;
		this.ollamaService = ollamaService;
	}

	@PostConstruct
	public void initialize() {
		assistantService.setConversationService(this);
	}

	@Override
	public void deleteConversationsForAssistant(int userId, int assistantId) {
		Assistant assistant = assistantService.findAssistant(userId, assistantId);

		MintyOllamaModel model;
		try {
			model = MintyOllamaModel.valueOf(assistant.model());
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model() + ". Cannot continue");
			return;
		}

		ChatMemoryRepository chatMemoryRepository = ollamaService.getChatMemoryRepository(model);

		List<String> chats = chatMemoryRepository.findConversationIds();
		chats.stream().filter(chat -> {
			String[] split = chat.split(":");
			if (split.length > 2) {
				int convoAssistantId = getAssistantIdFromConversationId(chat);
				String convoUserName = getUserNameFromConversationId(chat);
				String userName = userService.getUsernameFromId(userId);
				return assistantId == convoAssistantId && userName == convoUserName;
			}
			return false;
		}).forEach(chat -> chatMemoryRepository.deleteByConversationId(chat));

	}

	@Override
	public String getUserNameFromConversationId(String conversationId) {
		return conversationId.split(":")[0];
	}

	@Override
	public int getAssistantIdFromConversationId(String conversationId) {
		return Integer.parseInt(conversationId.split(":")[1]);
	}

	@Override
	public String getDefaultConversationId(String username) {
		return username + ":0:default";
	}

	@Override
	public List<List<String>> getConversationsForWorkflow(String workflowName) {
		ChatMemoryRepository chatMemoryRepository = ollamaService
				.getChatMemoryRepository(ollamaService.getDefaultModel());

		List<String> chats = chatMemoryRepository.findConversationIds();

		if (chats.isEmpty()) {
			return List.of();
		}

		chats = chats.stream().filter(chat -> {
			String[] split = chat.split(":");
			if (split.length > 2) {
				String convoWorkflowName = getWorkflowFromConversationId(chat);
				return workflowName.compareTo(convoWorkflowName) == 0;
			}
			return false;
		}).toList();

		List<List<String>> chatMessages = chats.stream().map(chat -> {
			List<String> msgs = chatMemoryRepository.findByConversationId(chat).stream()
					.map(message -> message.getText()).toList();
			return msgs;
		}).toList();

		return chatMessages;
	}

	@Override
	public void deleteConversationsForWorkflow(int userId, String workflowName) {
		ChatMemoryRepository chatMemoryRepository = ollamaService
				.getChatMemoryRepository(ollamaService.getDefaultModel());

		List<String> chats = chatMemoryRepository.findConversationIds();
		chats.stream().filter(chat -> {
			String[] split = chat.split(":");
			if (split.length > 2) {
				String convoWorkflowName = getWorkflowFromConversationId(chat);
				return workflowName.compareTo(convoWorkflowName) == 0;
			}
			return false;
		}).forEach(chat -> {
			logger.info("Deleting workflow conversation " + chat);
			chatMemoryRepository.deleteByConversationId(chat);
		});
	}

	@Override
	public boolean conversationOwnedBy(String conversationId, String username) {
		String convoUserId = getUserNameFromConversationId(conversationId);
		return username.equals(convoUserId);
	}

	@Override
	public String newConversationId(String username, int assistantId) {
		return username + ":" + assistantId + ":" + UUID.randomUUID().toString();
	}

	@Override
	public String newConversationId(int userId, int assistantId, String workflowName) {
		String username = userService.getUsernameFromId(userId);
		return username + ":" + assistantId + ":" + workflowName;
	}

	private String getWorkflowFromConversationId(String conversationId) {
		return conversationId.split(":")[2];
	}
}
