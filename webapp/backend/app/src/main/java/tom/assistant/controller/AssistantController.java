package tom.assistant.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import tom.api.services.ConversationService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.ollama.service.OllamaService;

@Controller
@RequestMapping("/api/assistant")
public class AssistantController {

	private static final Logger logger = LogManager.getLogger(AssistantController.class);

	private final MetadataService metadataService;
	private final AssistantManagementService assistantManagementService;
	private final OllamaService ollamaService;
	private final ConversationService conversationService;
	private final AssistantQueryService assistantQueryService;

	public AssistantController(AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, MetadataService metadataService, OllamaService ollamaService,
			ConversationService conversationService) {
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
		List<String> models = ollamaService.listModels().stream().map(model -> model.toString()).toList();
		ResponseWrapper<List<String>> response = ResponseWrapper.SuccessResponse(models);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/get" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") int assistantId) {
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
			@RequestParam("conversationId") String conversationId) {
		int assistantId = conversationService.getAssistantIdFromConversationId(conversationId);
		return getAssistant(user, assistantId);
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") int assistantId) {

		boolean success = assistantManagementService.deleteAssistant(user.getId(), assistantId);

		if (!success) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
					List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/ask" }, method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
	public StreamingResponseBody ask(@AuthenticationPrincipal UserDetailsUser user, @RequestBody AssistantQuery query) {
		Assistant assistant = assistantManagementService.findAssistant(user.getId(), query.getAssistantId());
		if (assistant == null) {
			return null;
		}

		Stream<String> responseStream = assistantQueryService.askStreaming(user.getId(), query);
		return outputStream -> {
			responseStream.forEach(item -> {
				try {
					outputStream.write((item).getBytes(StandardCharsets.UTF_8));
					outputStream.flush();
				} catch (IOException e) {
					logger.error("Caught exception while streaming assistant response: ", e);
					throw new RuntimeException("Error writing to stream", e);
				}
			});
		};

	}
}
