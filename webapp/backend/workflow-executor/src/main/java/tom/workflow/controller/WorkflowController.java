package tom.workflow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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
import tom.api.WorkflowId;
import tom.api.model.user.ResourceSharingSelection;
import tom.api.model.user.UserSelection;
import tom.api.services.WorkflowService;
import tom.api.services.exception.NotOwnedException;
import tom.api.services.workflow.Workflow;
import tom.api.services.workflow.WorkflowDescription;
import tom.api.services.workflow.WorkflowRequest;
import tom.api.task.enumspec.EnumSpec;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;
import tom.task.model.OutputTaskSpecDescription;
import tom.task.model.TaskSpecDescription;
import tom.task.registry.TaskRegistryService;

@Controller
@RequestMapping("/api/workflow")
public class WorkflowController {

	private final WorkflowService workflowService;
	private final TaskRegistryService taskRegistryService;
	private final MetadataService metadataService;

	public WorkflowController(WorkflowService workflowService, TaskRegistryService taskRegistryService,
			MetadataService metadataService) {
		this.workflowService = workflowService;
		this.taskRegistryService = taskRegistryService;
		this.metadataService = metadataService;
	}

	@GetMapping(value = { "" })
	public ResponseEntity<ResponseWrapper<Workflow>> getWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("workflowId") WorkflowId workflowId) {

		Workflow workflow = workflowService.getWorkflow(user.getId(), workflowId);
		if (workflow == null) {
			ResponseWrapper<Workflow> response = ResponseWrapper.ApiFailureResponse(HttpStatus.OK.value(),
					List.of(ApiError.NOT_FOUND));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(workflow);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping(value = { "" })
	public ResponseEntity<ResponseWrapper<Boolean>> deleteWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("workflowId") WorkflowId workflowId) {

		try {
			workflowService.deleteWorkflow(user.getId(), workflowId);
			ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

	}

	@PostMapping(value = { "/share" })
	public ResponseEntity<ResponseWrapper<String>> shareWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody() ResourceSharingSelection selection) {
		try {
			workflowService.shareWorkflow(user.getId(), selection);
			ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("Workflow shared.");
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@GetMapping(value = { "/getsharing" })
	public ResponseEntity<ResponseWrapper<UserSelection>> getSharing(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("name") String name) {
		try {
			UserSelection selection = workflowService.getSharingFor(user.getId(), name);
			ResponseWrapper<UserSelection> response = ResponseWrapper.SuccessResponse(selection);
			return new ResponseEntity<>(response, HttpStatus.OK);

		} catch (Exception e) {
			ResponseWrapper<UserSelection> response = ResponseWrapper
					.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@DeleteMapping(value = { "/cancel" })
	public ResponseEntity<ResponseWrapper<Boolean>> cancelWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("name") String name) {
		try {
			workflowService.cancelWorkflow(user.getId(), name);
			ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (NotOwnedException e) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@PostMapping(value = { "/execute" })
	public ResponseEntity<ResponseWrapper<String>> executeWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody WorkflowRequest request) {

		try {
			String workflowName = workflowService.executeWorkflow(user.getId(), request);
			ResponseWrapper<String> response;
			if (workflowName == null) {
				response = ResponseWrapper.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						List.of(ApiError.FAILED_TO_START_WORKFLOW));
			} else {
				metadataService.workflowExecuted(user.getId());
				response = ResponseWrapper.SuccessResponse("Started workflow " + workflowName);
			}

			return new ResponseEntity<>(response, HttpStatus.OK);

		} catch (NotOwnedException e) {
			ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@PostMapping(value = { "/new" })
	public ResponseEntity<ResponseWrapper<Workflow>> newWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Workflow workflow) {

		Workflow createdWorkflow;
		try {
			createdWorkflow = workflowService.createWorkflow(user.getId(), workflow);
			metadataService.workflowCreated(user.getId());

			ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(createdWorkflow);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (NotOwnedException e) {
			ResponseWrapper<Workflow> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
					List.of(ApiError.WORKFLOW_NAME_ALREADY_EXISTS));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@PostMapping(value = { "/update" })
	public ResponseEntity<ResponseWrapper<Workflow>> updateWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Workflow workflow) {

		try {
			Workflow updatedWorkflow = workflowService.updateWorkflow(user.getId(), workflow);

			ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(updatedWorkflow);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (NotOwnedException e) {
			ResponseWrapper<Workflow> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
					List.of(ApiError.WORKFLOW_NOT_OWNED_OR_DOESNT_EXIST));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@GetMapping(value = { "/specification/list" })
	public ResponseEntity<ResponseWrapper<List<TaskSpecDescription>>> listTaskSpecifications(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<TaskSpecDescription> taskSpecList = taskRegistryService.getTaskDescriptions();
		ResponseWrapper<List<TaskSpecDescription>> response = ResponseWrapper.SuccessResponse(taskSpecList);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = { "/output/specification/list" })
	public ResponseEntity<ResponseWrapper<List<OutputTaskSpecDescription>>> listOutputTaskSpecifications(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<OutputTaskSpecDescription> taskSpecList = taskRegistryService.getOutputTaskDescriptions();
		ResponseWrapper<List<OutputTaskSpecDescription>> response = ResponseWrapper.SuccessResponse(taskSpecList);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = { "/list" })
	public ResponseEntity<ResponseWrapper<List<WorkflowDescription>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<WorkflowDescription> workflows = workflowService.listWorkflows(user.getId());

		ResponseWrapper<List<WorkflowDescription>> response = ResponseWrapper.SuccessResponse(workflows);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = { "/enum" })
	public ResponseEntity<ResponseWrapper<List<EnumSpec>>> listEnumerations(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<EnumSpec> enums = this.taskRegistryService.getEnumerations(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(enums), HttpStatus.OK);
	}

	@GetMapping(value = { "/help/task" })
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getTaskHelpFiles(
			@AuthenticationPrincipal UserDetailsUser user) {
		Map<String, String> enums = this.taskRegistryService.getTaskHelpFiles();
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(enums), HttpStatus.OK);
	}

	@GetMapping(value = { "/help/output" })
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getOutputHelpFiles(
			@AuthenticationPrincipal UserDetailsUser user) {
		Map<String, String> enums = this.taskRegistryService.getOutputHelpFiles();
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(enums), HttpStatus.OK);
	}
}
