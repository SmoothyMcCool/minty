package tom.project.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import tom.api.ProjectId;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;
import tom.api.model.project.Project;
import tom.api.services.ProjectService;
import tom.api.services.document.extract.DocumentExtractorService;
import tom.config.MintyConfiguration;
import tom.controller.ResponseWrapper;
import tom.document.service.DocumentServiceInternal;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/project")
public class ProjectController {

	private static final Logger logger = LogManager.getLogger(ProjectController.class);

	private final ProjectService projectService;
	private final DocumentServiceInternal documentService;
	private final DocumentExtractorService documentExtractorService;
	private final Path tempFileStore;

	public ProjectController(ProjectService projectService, DocumentServiceInternal documentService,
			MintyConfiguration mintyConfig, DocumentExtractorService documentExtractorService) {
		this.projectService = projectService;
		this.documentService = documentService;
		this.documentExtractorService = documentExtractorService;
		tempFileStore = mintyConfig.getConfig().fileStores().temp();
	}

	@PostMapping("/create")
	public ResponseEntity<ResponseWrapper<Project>> createProject(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Project project) {

		try {
			Project created = projectService.createProject(user.getId(), project.name());

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(created));

		} catch (Exception e) {
			logger.warn("Failed to create project.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_PROJECT)));
		}
	}

	@DeleteMapping("/delete")
	public ResponseEntity<ResponseWrapper<Boolean>> deleteProject(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId) {

		try {
			projectService.deleteProject(user.getId(), projectId);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(true));

		} catch (Exception e) {
			logger.warn("Failed to delete project.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_DELETE_PROJECT)));
		}
	}

	@GetMapping
	public ResponseEntity<ResponseWrapper<Project>> getProject(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId) {

		try {
			Project project = projectService.getProject(user.getId(), projectId);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(project));

		} catch (Exception e) {
			logger.warn("Failed to retrieve project.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ENUMERATE_PROJECT)));
		}
	}

	@GetMapping("/list")
	public ResponseEntity<ResponseWrapper<List<Project>>> listProjects(@AuthenticationPrincipal UserDetailsUser user) {

		try {
			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(projectService.listProjects(user.getId())));

		} catch (Exception e) {
			logger.warn("Failed to list projects.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_LIST_PROJECTS)));
		}
	}

	@GetMapping("/node")
	public ResponseEntity<ResponseWrapper<NodeContent>> readNode(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("path") String path) {

		try {
			NodeContent node = projectService.readNode(user.getId(), projectId, path);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(node));

		} catch (Exception e) {
			logger.warn("Failed to read node.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ENUMERATE_PROJECT)));
		}
	}

	@PostMapping("/node/file")
	public ResponseEntity<ResponseWrapper<NodeInfo>> writeFile(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestBody NodeContent node) {

		try {
			NodeInfo info = projectService.writeFile(user.getId(), projectId, node.getPath(), node.getFileType(),
					node.getContent());

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(info));

		} catch (Exception e) {
			logger.warn("Failed to write file.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY)));
		}
	}

	@PostMapping("/node/folder")
	public ResponseEntity<ResponseWrapper<NodeInfo>> createFolder(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("path") String path) {

		try {
			NodeInfo info = projectService.createFolder(user.getId(), projectId, path);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(info));

		} catch (Exception e) {
			logger.warn("Failed to create folder.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY)));
		}
	}

	@PostMapping("/node/move")
	public ResponseEntity<ResponseWrapper<NodeInfo>> moveNode(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("sourcePath") String sourcePath,
			@RequestParam("targetPath") String targetPath) {

		try {
			NodeInfo info = projectService.moveNode(user.getId(), projectId, sourcePath, targetPath);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(info));

		} catch (Exception e) {
			logger.warn("Failed to move node.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY)));
		}
	}

	@DeleteMapping("/node")
	public ResponseEntity<ResponseWrapper<Boolean>> deleteNode(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("path") String path) {

		try {
			projectService.deleteNode(user.getId(), projectId, path);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(true));

		} catch (Exception e) {
			logger.warn("Failed to delete node.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY)));
		}
	}

	@PostMapping("/node/meta")
	public ResponseEntity<ResponseWrapper<Boolean>> updateNodeMetadata(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("oldPath") String oldPath,
			@RequestParam("newPath") String newPath,
			@RequestParam(name = "fileType", required = false) String fileType) {

		try {

			FileType ft = fileType == null ? FileType.text : FileType.valueOf(fileType);

			projectService.updateNodeMetadata(user.getId(), projectId, oldPath, newPath, ft);

			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(true));

		} catch (Exception e) {
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY)));
		}
	}

	@GetMapping("/node/tree")
	public ResponseEntity<ResponseWrapper<List<NodeInfo>>> describeTree(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId) {

		try {
			return ResponseEntity
					.ok(ResponseWrapper.SuccessResponse(projectService.describeTree(user.getId(), projectId)));

		} catch (Exception e) {
			logger.warn("Failed to describe tree.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ENUMERATE_PROJECT)));
		}
	}

	@GetMapping("/node/children")
	public ResponseEntity<ResponseWrapper<List<NodeInfo>>> listChildren(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("path") String path) {

		try {
			return ResponseEntity
					.ok(ResponseWrapper.SuccessResponse(projectService.listChildren(user.getId(), projectId, path)));

		} catch (Exception e) {
			logger.warn("Failed to list children.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ENUMERATE_PROJECT)));
		}
	}

	@PostMapping(value = { "/node/convert/markdown" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> convertToMarkdown(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestPart("file") MultipartFile mpf) {

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

	@PostMapping(value = { "/node/convert/markdown/decompose" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> convertToMarkdownAndDecompose(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("projectId") ProjectId projectId,
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

	@PostMapping(value = { "/node/convert/markdown/summarize" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> convertToMarkdownAndDecomposeSummarize(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("projectId") ProjectId projectId,
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
			@RequestParam("projectId") ProjectId projectId, @RequestPart("file") MultipartFile mpf) {

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

	@PostMapping(value = "/node/import/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> importZip(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestPart("file") MultipartFile file) {

		try {
			projectService.importZip(user.getId(), projectId, file.getInputStream());
			return ResponseEntity.ok(ResponseWrapper.SuccessResponse("Zip imported successfully"));
		} catch (Exception e) {
			logger.warn("Failed to import zip.", e);
			return ResponseEntity
					.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY)));
		}
	}

	@GetMapping(value = "/node/export/zip")
	public ResponseEntity<ResponseWrapper<String>> exportZip(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId) {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try (ZipOutputStream zip = new ZipOutputStream(baos)) {

				List<NodeInfo> nodes = projectService.describeTree(user.getId(), projectId);

				for (NodeInfo node : nodes) {

					if (node.getType() == NodeType.Folder) {
						continue;
					}

					NodeContent content = projectService.readNode(user.getId(), projectId, node.getPath());

					ZipEntry entry = new ZipEntry(node.getPath().replaceFirst("^/", ""));
					zip.putNextEntry(entry);
					zip.write(content.getContent().getBytes(StandardCharsets.UTF_8));
					zip.closeEntry();
				}
			}

			String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
			return ResponseEntity.ok(ResponseWrapper.SuccessResponse(base64));

		} catch (IOException e) {
			return ResponseEntity.ok(ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ZIP_PROJECT)));
		}
	}

}
