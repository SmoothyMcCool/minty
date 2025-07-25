package tom.conversation.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.ollama.service.OllamaService;
import tom.task.model.Assistant;
import tom.task.services.AssistantService;
import tom.task.services.ConversationService;
import tom.user.service.UserService;

@Service
public class ConversationServiceImpl implements ConversationService {

	private static final Logger logger = LogManager.getLogger(ConversationServiceImpl.class);

	private final AssistantService assistantService;
	private final UserService userService;
	private final OllamaService ollamaService;

	public ConversationServiceImpl(UserService userService, AssistantService assistantService,
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

		OllamaModel model;
		try {
			model = OllamaModel.valueOf(assistant.model());
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
				String convoUserName = getUserIdFromConversationId(chat);
				String userName = userService.getUsernameFromId(userId);
				return assistantId == convoAssistantId && userName == convoUserName;
			}
			return false;
		}).forEach(chat -> chatMemoryRepository.deleteByConversationId(chat));

	}

	@Override
	public String getUserIdFromConversationId(String conversationId) {
		return conversationId.split(":")[0];
	}

	@Override
	public int getAssistantIdFromConversationId(String conversationId) {
		return Integer.parseInt(conversationId.split(":")[1]);
	}

	@Override
	public String getDefaultConversationId(int userId) {
		String username = userService.getUsernameFromId(userId);
		return username + ":0:default";
	}

	@Override
	public String newConversationId(int userId, String assistantId) {
		return userId + ":" + assistantId + ":" + UUID.randomUUID().toString();
	}

	@Override
	public String newConversationId(int userId, int assistantId, String conversationId) {
		return userId + ":" + assistantId + ":" + conversationId;
	}

}
