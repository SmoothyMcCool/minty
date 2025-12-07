package tom.diagramming.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import tom.ApiError;
import tom.api.ConversationId;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StringResult;
import tom.assistant.service.management.AssistantManagementServiceInternal;
import tom.controller.ResponseWrapper;
import tom.conversation.model.Conversation;
import tom.conversation.service.ConversationServiceInternal;
import tom.meta.service.MetadataService;
import tom.model.AssistantQuery;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/diagram")
public class DiagrammingController {

	private final MetadataService metadataService;
	private final AssistantManagementServiceInternal assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final ConversationServiceInternal conversationService;

	public DiagrammingController(AssistantManagementServiceInternal assistantManagementService,
			AssistantQueryService assistantQueryService, MetadataService metadataService,
			ConversationServiceInternal conversationService) {
		this.assistantManagementService = assistantManagementService;
		this.metadataService = metadataService;
		this.assistantQueryService = assistantQueryService;
		this.conversationService = conversationService;
	}

	@GetMapping({ "ask" })
	public ResponseEntity<ResponseWrapper<ConversationId>> getDiagrammingAssistant(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("request") String request) {

		AssistantQuery query = new AssistantQuery();
		query.setAssistantId(AssistantManagementService.DiagrammingAssistantId);
		Conversation conversation = conversationService.newConversation(user.getId(), query.getAssistantId());
		query.setConversationId(conversation.getConversationId());
		query.setQuery(request);

		ConversationId requestId;
		try {
			requestId = assistantQueryService.ask(user.getId(), query);
		} catch (QueueFullException | ConversationInUseException e) {
			ResponseWrapper<ConversationId> response = ResponseWrapper
					.ApiFailureResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), List.of(ApiError.LLM_BUSY));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<ConversationId> response = ResponseWrapper.SuccessResponse(requestId);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "get" })
	public ResponseEntity<ResponseWrapper<String>> getResponse(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("requestId") ConversationId requestId) {

		StringResult llmResult = (StringResult) assistantQueryService.getResultFor(requestId);

		if (llmResult == null) {
			int queuePosition = assistantQueryService.getQueuePositionFor(requestId);
			// -1 means nothing is running. 0 means it is being processed by the LLM.
			if (queuePosition == -1) {
				ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("~~Not~Running~~");
				return new ResponseEntity<>(response, HttpStatus.OK);
			}

			ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("~~No~Response~~");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		if (llmResult == LlmResult.IN_PROGRESS) {
			ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("~~LLM~Executing~~");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		if (!llmResult.isComplete()) {
			ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("~~LLM~Executing~~");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		String llmResponse = llmResult.getValue();
		conversationService.deleteConversation(user.getId(), requestId);
		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse(llmResponse);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

}
