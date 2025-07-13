package tom.assistant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import tom.ApiError;
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.task.model.Assistant;
import tom.task.model.AssistantQuery;
import tom.task.services.AssistantService;

@Controller
@RequestMapping("/api/assistant")
public class AssistantController {

	private final AssistantService assistantService;

	public AssistantController(AssistantService assistantService) {

		this.assistantService = assistantService;
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Assistant>> addAssistant(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Assistant assistant) {

		assistant = assistantService.createAssistant(user.getId(), assistant);

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

	@RequestMapping(value = { "/get" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistant(@AuthenticationPrincipal UserDetailsUser user, @RequestParam("id") int assistantId) {
		Assistant assistant = assistantService.findAssistant(user.getId(), assistantId);

		if (assistant == null) {
			ResponseWrapper<Assistant> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		ResponseWrapper<Assistant> response = ResponseWrapper.SuccessResponse(assistant);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/conversation" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Assistant>> getAssistantForChat(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("conversationId") String conversationId) {
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

	@RequestMapping(value = { "/ask" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<String>> ask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody AssistantQuery query) {

		String chatResponse = assistantService.ask(user.getId(), query);

		ResponseWrapper<String> response;
		if (chatResponse != null) {
			response = ResponseWrapper.SuccessResponse(chatResponse);
		} else {
			response = ResponseWrapper.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
