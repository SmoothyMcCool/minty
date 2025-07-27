package tom.assistant.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

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
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.ollama.service.OllamaService;
import tom.task.services.AssistantService;

@Controller
@RequestMapping("/api/assistant")
public class AssistantController {

	private final MetadataService metadataService;
	private final AssistantService assistantService;
	private final OllamaService ollamaService;

	public AssistantController(AssistantService assistantService, MetadataService metadataService,
			OllamaService ollamaService) {
		this.assistantService = assistantService;
		this.metadataService = metadataService;
		this.ollamaService = ollamaService;
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Assistant>> addAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Assistant assistant) {

		assistant = assistantService.createAssistant(user.getId(), assistant);
		metadataService.newAssistant(user.getId());

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(assistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<Assistant>>> listAssistants(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<Assistant> assistants = assistantService.listAssistants(user.getId());
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
		Assistant assistant = assistantService.findAssistant(user.getId(), assistantId);

		if (assistant.Null()) {
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
		int assistantId = Integer.parseInt(conversationId.split(":")[1]);
		return getAssistant(user, assistantId);
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("id") int assistantId) {

		boolean success = assistantService.deleteAssistant(user.getId(), assistantId);

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
		Assistant assistant = assistantService.findAssistant(user.getId(), query.getAssistantId());
		if (assistant.Null()) {
			return null;
		}

		Stream<String> responseStream = assistantService.askStreaming(user.getId(), query);
		return outputStream -> {
			responseStream.forEach(item -> {
				try {
					outputStream.write((item).getBytes(StandardCharsets.UTF_8));
					outputStream.flush();
				} catch (IOException e) {
					throw new RuntimeException("Error writing to stream", e);
				}
			});
		};

	}
}
