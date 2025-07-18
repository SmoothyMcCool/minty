package tom.conversation.service;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

import tom.task.services.ConversationService;
import tom.user.service.UserService;

@Service
public class ConversationServiceImpl implements ConversationService {

	private final ChatMemoryRepository chatMemoryRepository;
	private final UserService userService;

	public ConversationServiceImpl(ChatMemoryRepository chatMemoryRepository, UserService userService) {
		this.chatMemoryRepository = chatMemoryRepository;
		this.userService = userService;
	}

	@Override
	public void deleteConversationsForAssistant(int id) {
		List<String> chats = chatMemoryRepository.findConversationIds();
		chats.stream().filter(chat -> {
			String[] split = chat.split(":");
			if (split.length > 2) {
				int assistantId = Integer.parseInt(chat.split(":")[1]);
				return id == assistantId;
			}
			return false;
		}).forEach(chat -> chatMemoryRepository.deleteByConversationId(chat));

	}

	@Override
	public String getDefaultConversationId(int userId) {
		String username = userService.getUsernameFromId(userId);
		return username + "-default";
	}

}
