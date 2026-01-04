package tom.project.controller;

import java.util.List;

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

import tom.ApiError;
import tom.api.ProjectEntryId;
import tom.api.ProjectId;
import tom.api.model.project.Project;
import tom.api.model.project.ProjectEntry;
import tom.api.model.project.ProjectEntryInfo;
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

	@GetMapping({ "" })
	public ResponseEntity<ResponseWrapper<Project>> getProject(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId) {

		ResponseWrapper<Project> response;

		try {
			Project newProject = projectService.getProject(user.getId(), projectId);
			response = ResponseWrapper.SuccessResponse(newProject);
		} catch (Exception e) {
			logger.warn("Failed to list project contents.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_PROJECT));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/list" })
	public ResponseEntity<ResponseWrapper<List<Project>>> listProjects(@AuthenticationPrincipal UserDetailsUser user) {

		ResponseWrapper<List<Project>> response;

		try {
			List<Project> projects = projectService.listProjects(user.getId());
			response = ResponseWrapper.SuccessResponse(projects);
		} catch (Exception e) {
			logger.warn("Failed to list project contents.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_LIST_PROJECTS));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping({ "/create" })
	public ResponseEntity<ResponseWrapper<Project>> addProject(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Project project) {

		ResponseWrapper<Project> response;

		try {
			Project newProject = projectService.createProject(user.getId(), project.name());
			response = ResponseWrapper.SuccessResponse(newProject);
		} catch (Exception e) {
			logger.warn("Failed to list project contents.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_PROJECT));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping({ "/delete" })
	public ResponseEntity<ResponseWrapper<Boolean>> deleteProject(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId) {

		ResponseWrapper<Boolean> response;

		try {
			projectService.deleteProject(user.getId(), projectId);
			response = ResponseWrapper.SuccessResponse(true);
		} catch (Exception e) {
			logger.warn("Failed to list project contents.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_PROJECT));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/entries" })
	public ResponseEntity<ResponseWrapper<List<ProjectEntryInfo>>> listProjectContents(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("projectId") ProjectId projectId) {

		ResponseWrapper<List<ProjectEntryInfo>> response;

		try {
			List<ProjectEntryInfo> projectFiles = projectService.listProjectEntries(user.getId(), projectId);
			response = ResponseWrapper.SuccessResponse(projectFiles);
		} catch (Exception e) {
			logger.warn("Failed to list project contents.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ENUMERATE_PROJECT));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping({ "/entry" })
	public ResponseEntity<ResponseWrapper<ProjectEntry>> getProjectEntry(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("entry") ProjectEntryId entryId) {

		ResponseWrapper<ProjectEntry> response;

		try {
			ProjectEntry projectEntry = projectService.getProjectEntry(user.getId(), projectId, entryId);
			response = ResponseWrapper.SuccessResponse(projectEntry);
		} catch (Exception e) {
			logger.warn("Failed to retrieve project entry.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_ENUMERATE_PROJECT));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = "/entry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<Boolean>> createOrUpdateProjectEntry(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("projectId") ProjectId projectId,
			@RequestPart("entry") ProjectEntry entry) {

		ResponseWrapper<Boolean> response;

		try {
			projectService.createOrUpdateProjectEntry(user.getId(), projectId, entry);
			response = ResponseWrapper.SuccessResponse(true);
		} catch (Exception e) {
			logger.warn("Failed to create project entry.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping(value = "/entry")
	public ResponseEntity<ResponseWrapper<Boolean>> deleteProjectEntry(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("projectId") ProjectId projectId, @RequestParam("entry") ProjectEntryId entryId) {

		ResponseWrapper<Boolean> response;

		try {
			projectService.deleteProjectEntry(user.getId(), projectId, entryId);
			response = ResponseWrapper.SuccessResponse(true);
		} catch (Exception e) {
			logger.warn("Failed to create project entry.", e);
			response = ResponseWrapper.ApiFailureResponse(500, List.of(ApiError.FAILED_TO_CREATE_OR_UPDATE_ENTRY));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
