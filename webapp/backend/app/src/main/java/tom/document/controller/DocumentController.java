package tom.document.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import tom.ApiError;
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.document.model.MintyDoc;
import tom.document.service.DocumentService;

@Controller
@RequestMapping("/api/document")
public class DocumentController {

	private static final Logger logger = LogManager.getLogger(DocumentController.class);

	@Value("${docFileStore}")
	private String docFileStore;
	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@RequestMapping(value = { "/add" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<MintyDoc>> addDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody MintyDoc document) {

		boolean exists = documentService.documentExists(document.getDocumentId());

		document.setDocumentId(UUID.randomUUID());
		if (exists) {
			ResponseWrapper<MintyDoc> response = ResponseWrapper.ApiFailureResponse(HttpStatus.CONFLICT.value(),
					List.of(ApiError.DOCUMENT_ALREADY_EXISTS));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		MintyDoc addedDoc = documentService.addDocument(user.getId(), document);
		ResponseWrapper<MintyDoc> response = ResponseWrapper.SuccessResponse(addedDoc);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/upload" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> uploadDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("documentId") String documentId, @RequestPart("file") MultipartFile mpf) {

		File file = new File(docFileStore + "/" + documentId);
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

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<MintyDoc>>> listDocuments(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<MintyDoc> documents = documentService.listDocuments();

		ResponseWrapper<List<MintyDoc>> response = ResponseWrapper.SuccessResponse(documents);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/delete" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("documentId") UUID documentId) {

		if (!documentService.deleteDocument(user.getId(), documentId)) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(), List.of(ApiError.NOT_OWNED)),
					HttpStatus.OK);
		}

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(true), HttpStatus.OK);
	}

}
