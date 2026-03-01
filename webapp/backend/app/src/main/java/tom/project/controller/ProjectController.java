package tom.project.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import tom.ApiError;
import tom.api.ProjectId;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.Project;
import tom.api.services.ProjectService;
import tom.controller.ResponseWrapper;
import tom.model.security.UserDetailsUser;

@Controller
@RequestMapping("/api/project")
public class ProjectController {

	private static final Logger logger = LogManager.getLogger(ProjectController.class);

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
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
}
