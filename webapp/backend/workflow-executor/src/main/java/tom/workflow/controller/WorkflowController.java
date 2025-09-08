package tom.workflow.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import tom.ApiError;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;
import tom.workflow.model.ResultTemplate;
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

	@GetMapping(value = { "/list" })
	public ResponseEntity<ResponseWrapper<List<Workflow>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<Workflow> workflows = workflowService.listWorkflows(user.getId());

		ResponseWrapper<List<Workflow>> response = ResponseWrapper.SuccessResponse(workflows);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(value = { "/resultTemplate" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseWrapper<String>> addNewResultTemplate(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("templateName") String templateName, @RequestPart("file") MultipartFile mpf)
			throws IOException {

		if (mpf.isEmpty()) {
			return new ResponseEntity<>(
					ResponseWrapper.FailureResponse(HttpStatus.BAD_REQUEST.value(), "File is empty."), HttpStatus.OK);
		}

		boolean preExisting = false;
		UUID templateId = null;

		// Is there already a template with this name?
		ResultTemplate preExistingTemplate = workflowService.getResultTemplate(templateName);
		if (preExistingTemplate != null) {
			// Is it owned by this user?
			if (!preExistingTemplate.getOwnerId().equals(user.getId())) {
				return new ResponseEntity<>(ResponseWrapper.FailureResponse(HttpStatus.BAD_REQUEST.value(),
						"You cannot update a template you don't own."), HttpStatus.OK);
			}
			preExisting = true;
			templateId = preExistingTemplate.getId();
		}
		byte[] bytes = mpf.getBytes();
		String content = new String(bytes, StandardCharsets.UTF_8);

		String name = workflowService
				.addorUpdateResultTemplate(new ResultTemplate(templateId, user.getId(), templateName, content));

		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse(preExisting ? "Updated content of template " + name : "Added template " + name);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = { "/resultTemplate" })
	public ResponseEntity<ResponseWrapper<List<String>>> listResultTemplate(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<String> templateNames = workflowService.listResultTemplates();
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(templateNames), HttpStatus.OK);
	}

}
