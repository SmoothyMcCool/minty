package tom.document.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import tom.ApiError;
import tom.api.DocumentId;
import tom.api.ProjectId;
import tom.api.model.project.FileType;
import tom.api.services.ProjectService;
import tom.api.services.document.extract.DocumentExtractorService;
import tom.config.MintyConfiguration;
import tom.controller.ResponseWrapper;
import tom.document.model.MintyDoc;
import tom.document.service.DocumentServiceInternal;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/document")
public class DocumentController {

	private static final Logger logger = LogManager.getLogger(DocumentController.class);

	private final DocumentServiceInternal documentService;
	private final DocumentExtractorService documentExtractorService;
	private final ProjectService projectService;
	private final Path docFileStore;
	private final Path tempFileStore;

	public DocumentController(DocumentServiceInternal documentService,
			DocumentExtractorService documentExtractorService, ProjectService projectService,
			MintyConfiguration properties) {
		this.documentService = documentService;
		this.documentExtractorService = documentExtractorService;
		this.projectService = projectService;
		docFileStore = properties.getConfig().fileStores().docs();
		tempFileStore = properties.getConfig().fileStores().temp();
	}

	@PostMapping({ "/add" })
	public ResponseEntity<ResponseWrapper<MintyDoc>> addDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody MintyDoc document) {

		boolean exists = documentService.documentExists(document);

		if (exists) {
			ResponseWrapper<MintyDoc> response = ResponseWrapper.ApiFailureResponse(HttpStatus.CONFLICT.value(),
					List.of(ApiError.DOCUMENT_ALREADY_EXISTS));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		document.setDocumentId(new DocumentId(UUID.randomUUID()));
		MintyDoc addedDoc = documentService.addDocument(user.getId(), document);
		ResponseWrapper<MintyDoc> response = ResponseWrapper.SuccessResponse(addedDoc);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = { "/upload" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> uploadDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam String documentId, @RequestPart("file") MultipartFile mpf) {

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

	@GetMapping({ "/list" })
	public ResponseEntity<ResponseWrapper<List<MintyDoc>>> listDocuments(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam ProjectId projectId) {
		List<MintyDoc> documents = documentService.listDocuments(user.getId(), projectId);

		ResponseWrapper<List<MintyDoc>> response = ResponseWrapper.SuccessResponse(documents);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping({ "/delete" })
	public ResponseEntity<ResponseWrapper<Boolean>> deleteDocument(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam DocumentId documentId) {

		if (!documentService.deleteDocument(user.getId(), documentId)) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(), List.of(ApiError.NOT_OWNED)),
					HttpStatus.OK);
		}

		logger.info("User " + user.getId() + "deleted document " + documentId);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(true), HttpStatus.OK);
	}

	@PostMapping(value = { "/convert/markdown" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> addDocumentAndConvertToMarkdown(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam ProjectId projectId,
			@RequestPart("file") MultipartFile mpf) {

		File file = new File(tempFileStore + "/" + mpf.getOriginalFilename());
		try {
			Files.createDirectories(file.toPath());
			mpf.transferTo(file);

			try {
				String filename = file.getName();
				int lastDot = filename.lastIndexOf('.');
				String baseName = (lastDot == -1) ? filename : filename.substring(0, lastDot);
				String newName = baseName + ".md";
				logger.info("Started processing " + newName);

				String markdown = documentExtractorService.extract(file);

				projectService.writeFile(user.getId(), projectId, "/" + newName, FileType.markdown, markdown);
				logger.info("Markdown processing complete for " + file.getName());

				ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("File processed successfully.");
				return new ResponseEntity<>(response, HttpStatus.OK);

			} catch (Exception e) {
				logger.error("Markdown processing failed: ", e);
			} finally {
				file.delete();
			}

		} catch (IllegalStateException | IOException e) {
			logger.error("Failed to store file: ", e);
			ResponseWrapper<String> response = ResponseWrapper
					.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse("You'll find your file in the active project.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = { "/convert/markdown/decompose" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> addDocumentAndConvertToMarkdownAndDecompose(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam ProjectId projectId,
			@RequestPart("file") MultipartFile mpf) {

		File file = new File(tempFileStore + "/" + mpf.getOriginalFilename());

		try {
			Files.createDirectories(file.toPath());
			mpf.transferTo(file);

			logger.info("Markdown processing started for " + file.getName());
			documentService.processFileToMarkdownAndDecompose(user.getId(), projectId, file, projectService, false);

		} catch (Exception e) {
			logger.error("Markdown processing failed: ", e);
			ResponseWrapper<String> response = ResponseWrapper
					.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			file.delete();
		}

		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse("Markdown conversion and decomposing complete.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = { "/convert/markdown/summarize" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> addDocumentAndConvertToMarkdownAndDecomposeSummarize(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam ProjectId projectId,
			@RequestPart("file") MultipartFile mpf) {

		File file = new File(tempFileStore + "/" + mpf.getOriginalFilename());

		try {
			Files.createDirectories(file.toPath());
			mpf.transferTo(file);

			logger.info("Markdown processing started for " + file.getName());
			documentService.processFileToMarkdownAndDecompose(user.getId(), projectId, file, projectService, true);
			// Don't delete the file. Processing task will do that when it completes.

		} catch (Exception e) {
			logger.error("Markdown processing failed: ", e);
			ResponseWrapper<String> response = ResponseWrapper
					.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of(ApiError.REQUEST_FAILED));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse(
				"Markdown conversion and decomposing complete. Summarizing will take some time. Check back later.");
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@SuppressWarnings("unused")
	@PostMapping(value = { "/node/convert/mermaid" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> convertToMermaid(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam ProjectId projectId, @RequestPart("file") MultipartFile mpf) {

		ResponseWrapper<String> response;

		if (false) {
			File file = new File(tempFileStore + "/" + mpf.getOriginalFilename());
			try {
				Files.createDirectories(file.toPath());
				mpf.transferTo(file);

				try {
					String filename = file.getName();
					int lastDot = filename.lastIndexOf('.');
					String baseName = (lastDot == -1) ? filename : filename.substring(0, lastDot);
					String newName = baseName + ".md";
					logger.info("Started processing " + newName);

					String markdown = documentExtractorService.extract(file);

					projectService.writeFile(user.getId(), projectId, "/" + newName, FileType.markdown, markdown);
					logger.info("Markdown processing complete for " + file.getName());

					response = ResponseWrapper.SuccessResponse("File processed successfully.");
					return new ResponseEntity<>(response, HttpStatus.OK);

				} catch (Exception e) {
					logger.error("Markdown processing failed: ", e);
				} finally {
					file.delete();
				}

			} catch (IllegalStateException | IOException e) {
				logger.error("Failed to store file: ", e);
				response = ResponseWrapper.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						List.of(ApiError.REQUEST_FAILED));
				return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			response = ResponseWrapper.SuccessResponse("You'll find your file in the active project.");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		response = ResponseWrapper.SuccessResponse("Mermaid conversion isn't fully implemented yet.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = "/tasks")
	public ResponseEntity<ResponseWrapper<List<String>>> getInProgressTasks(
			@AuthenticationPrincipal UserDetailsUser user) {
		ResponseWrapper<List<String>> response = ResponseWrapper
				.SuccessResponse(documentService.getInProgressTaskNames(user.getId()));
		return ResponseEntity.ok(response);
	}

}
