package tom.conversation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import tom.ApiError;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.controller.ResponseWrapper;
import tom.conversation.model.Conversation;
import tom.conversation.service.ConversationServiceInternal;
import tom.meta.service.MetadataService;
import tom.model.ChatMessage;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/conversation")
public class ConversationController {

	private final ConversationServiceInternal conversationService;
	private final MetadataService metadataService;

	public ConversationController(ConversationServiceInternal conversationService, MetadataService metadataService) {
		this.conversationService = conversationService;
		this.metadataService = metadataService;
	}

	@RequestMapping(value = { "new" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Conversation>> startNewConversation(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("assistantId") AssistantId assistantId) {
		ResponseWrapper<Conversation> response = ResponseWrapper
				.SuccessResponse(conversationService.newConversation(user.getId(), assistantId));
		metadataService.newConversation(user.getId());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<Conversation>>> listChats(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<Conversation> conversations = conversationService.listConversationsForUser(user.getId());

		ResponseWrapper<List<Conversation>> response = ResponseWrapper.SuccessResponse(conversations);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/history" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<ChatMessage>>> getChatHistory(
			@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") ConversationId conversationId) {

		List<ChatMessage> messages = conversationService.getChatMessages(user.getId(), conversationId);

		ResponseWrapper<List<ChatMessage>> response = ResponseWrapper.SuccessResponse(messages);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<String>> deleteConversation(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") ConversationId conversationId) {

		if (!conversationService.deleteConversation(user.getId(), conversationId)) {
			ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("success");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/reset" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<String>> resetConversation(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") ConversationId conversationId) {

		if (!conversationService.resetConversation(user.getId(), conversationId)) {
			ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("success");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/rename" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Conversation>> renameConversation(
			@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") ConversationId conversationId, @RequestParam("title") String title) {

		Conversation conversation = conversationService.renameConversation(user.getId(), conversationId, title);
		if (conversation == null) {
			ResponseWrapper<Conversation> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<Conversation> response = ResponseWrapper.SuccessResponse(conversation);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
