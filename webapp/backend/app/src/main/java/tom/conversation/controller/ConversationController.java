package tom.conversation.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.ollama.api.OllamaModel;
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
import tom.meta.service.MetadataService;
import tom.ollama.OllamaService;
import tom.task.model.Assistant;
import tom.task.services.AssistantService;
import tom.task.services.ConversationService;

@Controller
@RequestMapping("/api/conversation")
public class ConversationController {

	private final ConversationService conversationService;
	private final MetadataService metadataService;
	private final OllamaService ollamaService;
	private final AssistantService assistantService;

	public ConversationController(ConversationService conversationService, MetadataService metadataService,
			OllamaService ollamaService, AssistantService assistantService) {
		this.conversationService = conversationService;
		this.metadataService = metadataService;
		this.ollamaService = ollamaService;
		this.assistantService = assistantService;
	}

	@RequestMapping(value = { "new" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<String>> startNewConversation(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("assistantId") String assistantId) {
		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse(conversationService.newConversationId(user.getId(), assistantId));
		metadataService.newConversation(user.getId());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<String>>> listChats(@AuthenticationPrincipal UserDetailsUser user) {

		List<String> models = ollamaService.listModels().stream().map(model -> model.toString()).toList();
		// No need to cycle over all models. Since the repository is common, asking the
		// first one is enough.
		List<String> chats = ollamaService.getChatMemoryRepository(OllamaModel.valueOf(models.getFirst()))
				.findConversationIds();

		chats = chats.stream().filter(item -> item.startsWith(user.getId() + ":")).toList();

		ResponseWrapper<List<String>> response = ResponseWrapper.SuccessResponse(chats);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/history" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<ChatMessage>>> getChatHistory(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("conversationId") String conversationId) {

		String model = getModelforConversation(user.getId(), conversationId);

		List<ChatMessage> result = new ArrayList<>();

		if (model != null) {
			ChatMemory chatMemory = ollamaService.getChatMemory(OllamaModel.valueOf(model));

			List<Message> messages = chatMemory.get(conversationId);
			result = messages.stream()
					.map(message -> new ChatMessage(message.getMessageType() == MessageType.USER, message.getText()))
					.collect(Collectors.toList());
			Collections.reverse(result);
		}

		ResponseWrapper<List<ChatMessage>> response = ResponseWrapper.SuccessResponse(result);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private String getModelforConversation(int userId, String conversationId) {
		int assistantId = conversationService.getAssistantIdFromConversationId(conversationId);

		Assistant assistant = assistantService.findAssistant(userId, assistantId);
		if (assistant.isNull()) {
			return "";
		}

		return assistant.model();
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<String>> deleteConversation(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") String conversationId) {

		String model = getModelforConversation(user.getId(), conversationId);
		ChatMemory chatMemory = ollamaService.getChatMemory(OllamaModel.valueOf(model));

		chatMemory.clear(conversationId);

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("success");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/delete/default" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<String>> deleteDefaultConversation(
			@AuthenticationPrincipal UserDetailsUser user) {

		OllamaModel model = ollamaService.getDefaultModel();
		ChatMemory chatMemory = ollamaService.getChatMemory(model);

		chatMemory.clear(conversationService.getDefaultConversationId(user.getId()));

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("success");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
