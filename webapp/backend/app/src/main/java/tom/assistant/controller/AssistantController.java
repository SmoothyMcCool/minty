package tom.assistant.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import tom.ApiError;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StreamResult;
import tom.controller.ResponseWrapper;
import tom.conversation.service.ConversationServiceInternal;
import tom.meta.service.MetadataService;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.model.security.UserDetailsUser;
import tom.ollama.service.OllamaService;

@Controller
@RequestMapping("/api/assistant")
public class AssistantController {

	private final MetadataService metadataService;
	private final AssistantManagementService assistantManagementService;
	private final OllamaService ollamaService;
	private final ConversationServiceInternal conversationService;
	private final AssistantQueryService assistantQueryService;

	public AssistantController(AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, MetadataService metadataService, OllamaService ollamaService,
			ConversationServiceInternal conversationService) {
		this.assistantManagementService = assistantManagementService;
		this.metadataService = metadataService;
		this.ollamaService = ollamaService;
		this.conversationService = conversationService;
		this.assistantQueryService = assistantQueryService;
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Assistant>> addAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Assistant assistant) {

		assistant = assistantManagementService.createAssistant(user.getId(), assistant);
		metadataService.newAssistant(user.getId());

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(assistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/edit" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Assistant>> editAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Assistant assistant) {

		assistant = assistantManagementService.updateAssistant(user.getId(), assistant);

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(assistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<Assistant>>> listAssistants(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<Assistant> assistants = assistantManagementService.listAssistants(user.getId());
		ResponseWrapper<List<Assistant>> response = ResponseWrapper.SuccessResponse(assistants);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/models" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<String>>> listModels(@AuthenticationPrincipal UserDetailsUser user) {
		List<String> models = ollamaService.listModels();
		ResponseWrapper<List<String>> response = ResponseWrapper.SuccessResponse(models);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/get" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") AssistantId assistantId) {
		Assistant assistant = assistantManagementService.findAssistant(user.getId(), assistantId);

		if (assistant == null) {
			ResponseWrapper<Assistant> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(assistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/conversation" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistantForChat(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("conversationId") ConversationId conversationId) {

		if (conversationService.conversationOwnedBy(user.getId(), conversationId)) {
			AssistantId assistantId = conversationService.getAssistantIdFromConversationId(user.getId(),
					conversationId);
			return getAssistant(user, assistantId);
		}

		ResponseWrapper<Assistant> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
				List.of(ApiError.NOT_OWNED));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") AssistantId assistantId) {

		boolean success = assistantManagementService.deleteAssistant(user.getId(), assistantId);

		if (!success) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
					List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/ask" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<ConversationId>> ask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody AssistantQuery query) {
		Assistant assistant = assistantManagementService.findAssistant(user.getId(), query.getAssistantId());
		if (assistant == null) {
			ResponseWrapper<ConversationId> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		if (!assistant.ownerId().equals(user.getId()) && !assistant.shared()) {
			ResponseWrapper<ConversationId> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		if (conversationService.conversationOwnedBy(user.getId(), query.getConversationId())) {
			ConversationId requestUuid;
			try {
				requestUuid = assistantQueryService.askStreaming(user.getId(), query);

			} catch (QueueFullException e) {
				ResponseWrapper<ConversationId> response = ResponseWrapper
						.ApiFailureResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), List.of(ApiError.LLM_BUSY));
				return new ResponseEntity<>(response, HttpStatus.OK);
			}

			ResponseWrapper<ConversationId> response = ResponseWrapper.SuccessResponse(requestUuid);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<ConversationId> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
				List.of(ApiError.NOT_OWNED));
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@RequestMapping(value = { "/response" }, method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
	public StreamingResponseBody ask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody ConversationId streamId) {

		LlmResult llmResult = assistantQueryService.getResultFor(streamId);
		StreamResult streamResult = (StreamResult) llmResult;

		if (streamResult == null) {
			int queuePosition = assistantQueryService.getQueuePositionFor(streamId);
			return outputStream -> {
				if (queuePosition == -1) {
					return; // Nothing is running, so end the stream.
				} else {
					outputStream.write(("~~Not~ready~~" + queuePosition).getBytes(StandardCharsets.UTF_8));
				}
				outputStream.flush();
			};
		}

		return outputStream -> {

			while (true) {
				String chunk;
				try {
					chunk = streamResult.takeChunk();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Streaming thread got interrupted. Aborting.");
				}
				if (chunk == null) {
					break;
				}

				outputStream.write((chunk).getBytes(StandardCharsets.UTF_8));
				outputStream.flush();
			}

		};

	}
}
