package tom.document.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import tom.ApiError;
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.task.model.Assistant;
import tom.task.services.AssistantService;
import tom.task.services.DocumentService;

@Controller
@RequestMapping("/api/document")
public class DocumentController {

	private static final Logger logger = LogManager.getLogger(DocumentController.class);

	@Value("${tempFileStore}")
	private String tempFileStore;
	private final AssistantService assistantService;
	private final DocumentService documentService;

	public DocumentController(DocumentService documentService, AssistantService assistantService) {
		this.assistantService = assistantService;
		this.documentService = documentService;
	}

	@RequestMapping(value = { "/add" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> addDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("assistantId") int assistantId, @RequestPart("file") MultipartFile mpf) {

		Assistant assistant = assistantService.findAssistant(user.getId(), assistantId);

		if (assistant.isNull()) {
			ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		File file = new File(tempFileStore + "/"
				+ documentService.constructFilename(user.getId(), assistantId, mpf.getOriginalFilename()));
		try {
			Files.createDirectories(file.toPath());
			mpf.transferTo(file);
			documentService.processFile(file);
		} catch (IllegalStateException | IOException e) {
			logger.error("Failed to store file: ", e);
			ResponseWrapper<String> response = ResponseWrapper
					.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// The file will get processed asynchronously.

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("ok");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
