package tom.workflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

	@RequestMapping(value = { "" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Boolean>> executeWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody WorkflowRequest request) {

		workflowService.executeWorkflow(user.getId(), request);
		metadataService.workflowExecuted(user.getId());

		ResponseWrapper<Boolean> response = ResponseWrapper.SuccessResponse(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Workflow>> newWorkflow(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Workflow workflow) {

		Workflow createdWorkflow = workflowService.createWorkflow(user.getId(), workflow);
		metadataService.workflowCreated(user.getId());

		ResponseWrapper<Workflow> response = ResponseWrapper.SuccessResponse(createdWorkflow);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<List<Workflow>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<Workflow> workflows = workflowService.listWorkflows(user.getId());

		ResponseWrapper<List<Workflow>> response = ResponseWrapper.SuccessResponse(workflows);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
