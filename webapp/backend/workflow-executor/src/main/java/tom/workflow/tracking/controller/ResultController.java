package tom.workflow.tracking.controller;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import tom.controller.ResponseWrapper;
import tom.model.security.UserDetailsUser;
import tom.workflow.tracking.controller.model.WorkflowResult;
import tom.workflow.tracking.controller.model.WorkflowState;
import tom.workflow.tracking.service.WorkflowTrackingService;

@Controller
@RequestMapping("/api/result")
public class ResultController {

	private final Logger logger = LogManager.getLogger(ResultController.class);

	private final WorkflowTrackingService workflowTrackingService;

	public ResultController(WorkflowTrackingService workflowTrackingService) {
		this.workflowTrackingService = workflowTrackingService;
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<WorkflowState>>> getResultList(
			@AuthenticationPrincipal UserDetailsUser user) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseWrapper<List<WorkflowState>> response = ResponseWrapper
				.SuccessResponse(workflowTrackingService.getWorkflowList(user.getId()));
		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<WorkflowResult>> getResult(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam(value = "workflowId") UUID workflowId) {
		logger.info("ResultController getResult: " + workflowId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseWrapper<WorkflowResult> response = ResponseWrapper
				.SuccessResponse(workflowTrackingService.getResult(user.getId(), workflowId));
		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	@RequestMapping(value = { "/output" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<String>> getOutput(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam(value = "workflowId") UUID workflowId) {
		logger.info("ResultController getOutput: " + workflowId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse(workflowTrackingService.getOutput(user.getId(), workflowId));
		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	@RequestMapping(value = { "/log" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<String>> getLog(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam(value = "workflowId") UUID workflowId) {
		logger.info("ResultController getLog: " + workflowId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseWrapper<String> response = ResponseWrapper
				.SuccessResponse(workflowTrackingService.getLog(user.getId(), workflowId));
		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteResult(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("workflowId") UUID workflowId) {
		workflowTrackingService.deleteResult(user.getId(), workflowId);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(true), HttpStatus.OK);
	}
}
