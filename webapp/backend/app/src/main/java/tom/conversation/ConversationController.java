package tom.conversation;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.conversation.repository.ChatMessage;
import tom.task.services.ConversationService;

@Controller
@RequestMapping("/api/conversation")
public class ConversationController {

	private final ConversationService conversationService;
	private final ChatMemory chatMemory;
	private final ChatMemoryRepository chatMemoryRepository;

	public ConversationController(ConversationService conversationService, ChatMemoryRepository chatMemoryRepository, ChatMemory chatMemory) {
		this.conversationService = conversationService;
		this.chatMemoryRepository = chatMemoryRepository;
		this.chatMemory = chatMemory;
	}

	@RequestMapping(value = { "new" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<String>> startNewConversation(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("assistantId") String assistantId) {
		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse(user.getId() + ":" + assistantId + ":" + UUID.randomUUID().toString());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<String>>> listChats(@AuthenticationPrincipal UserDetailsUser user) {
		List<String> chats = chatMemoryRepository.findConversationIds();

		chats = chats.stream().filter(item -> item.startsWith(user.getId() + ":")).toList();

		ResponseWrapper<List<String>> response = ResponseWrapper.SuccessResponse(chats);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/history" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<ChatMessage>>> getChatHistory(
			@RequestParam("conversationId") String conversationId) {

		List<Message> messages = chatMemory.get(conversationId);
		List<ChatMessage> result = messages.stream().map(message -> new ChatMessage(message.getMessageType() == MessageType.USER, message.getText())).collect(Collectors.toList());
		Collections.reverse(result);

		ResponseWrapper<List<ChatMessage>> response = ResponseWrapper.SuccessResponse(result);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<String>> deleteConversation(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") String conversationId) {

		chatMemory.clear(conversationId);

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("success");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/delete/default" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<String>> deleteDefaultConversation(
			@AuthenticationPrincipal UserDetailsUser user) {

		chatMemory.clear(conversationService.getDefaultConversationId(user.getId()));

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("success");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public void deleteConversationsForAssistant(int id) {
		conversationService.deleteConversationsForAssistant(id);
	}

}
