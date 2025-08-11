package tom.workflow.controller;

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
import tom.meta.service.MetadataService;
import tom.workflow.model.Workflow;
import tom.workflow.service.WorkflowService;

@Controller
@RequestMapping("/api/workflow")
public class WorkflowController {

	private final WorkflowService workflowService;
	private final MetadataService metadataService;

	public WorkflowController(WorkflowService workflowService, MetadataService metadataService) {
		this.workflowService = workflowService;
		this.metadataService = metadataService;
	}

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Workflow>> getWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("workflowId") int workflowId) {

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
			@RequestParam("workflowId") int workflowId) {

		if (!workflowService.isWorkflowOwned(workflowId, user.getId())) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		workflowService.deleteWorkflow(user.getId(), workflowId);
		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/execute" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Boolean>> executeWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody WorkflowRequest request) {

		if (!workflowService.isAllowedToExecute(request.getId(), user.getId())) {
			ResponseWrapper<Boolean> response = ResponseWrapper.ApiFailureResponse(HttpStatus.FORBIDDEN.value(),
					List.of(ApiError.NOT_OWNED));
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		workflowService.executeWorkflow(user.getId(), request);
		metadataService.workflowExecuted(user.getId());

		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
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

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<Workflow>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<Workflow> workflows = workflowService.listWorkflows(user.getId());

		ResponseWrapper<List<Workflow>> response = ResponseWrapper.SuccessResponse(workflows);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
