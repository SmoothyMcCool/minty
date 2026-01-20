package tom.workflow.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import tom.ApiError;
import tom.api.task.enumspec.EnumSpec;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;
import tom.task.model.OutputTaskSpecDescription;
import tom.task.model.TaskSpecDescription;
import tom.task.registry.TaskRegistryService;
import tom.workflow.model.Workflow;
import tom.workflow.service.WorkflowService;

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

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Workflow>> getWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("workflowId") UUID workflowId) {

		Workflow workflow = workflowService.getWorkflow(user.getId(), workflowId);
		if (workflow == null) {
			ResponseWrapper<Workflow> response = ResponseWrapper.ApiFailureResponse(HttpStatus.OK.value(),
					List.of(ApiError.NOT_FOUND));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(workflow);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("workflowId") UUID workflowId) {

		if (!workflowService.isWorkflowOwned(workflowId, user.getId())) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		workflowService.deleteWorkflow(user.getId(), workflowId);
		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "cancel" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> cancelWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("name") String name) {

		if (!workflowService.isWorkflowOwned(user.getId(), name)) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		workflowService.cancelWorkflow(user.getId(), name);
		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/execute" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<String>> executeWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody WorkflowRequest request) {

		if (!workflowService.isAllowedToExecute(request.getId(), user.getId())) {
			ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		String workflowName = workflowService.executeWorkflow(user.getId(), request);
		metadataService.workflowExecuted(user.getId());

		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse("Started workflow " + workflowName);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Workflow>> newWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Workflow workflow) {

		workflow.setOwnerId(user.getId());
		Workflow createdWorkflow = workflowService.createWorkflow(user.getId(), workflow);
		metadataService.workflowCreated(user.getId());

		ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(createdWorkflow);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/update" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Workflow>> updateWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Workflow workflow) {

		if (!workflowService.isWorkflowOwned(workflow.getId(), user.getId())) {
			ResponseWrapper<Workflow> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		Workflow updatedWorkflow = workflowService.updateWorkflow(user.getId(), workflow);

		ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(updatedWorkflow);
		return new ResponseEntity<>(response, HttpStatus.OK);
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
	public ResponseEntity<ResponseWrapper<List<Workflow>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<Workflow> workflows = workflowService.listWorkflows(user.getId());

		ResponseWrapper<List<Workflow>> response = ResponseWrapper.SuccessResponse(workflows);
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
