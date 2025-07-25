package tom.task.result.controller;

import java.io.IOException;
import java.util.List;

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

import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.task.executor.service.TaskExecutionService;

@Controller
@RequestMapping("/api/result/task")
public class TaskResultController {

	private final Logger logger = LogManager.getLogger(TaskResultController.class);

	private final TaskExecutionService taskExecutionService;

	public TaskResultController(TaskExecutionService taskExecutionService) {
		this.taskExecutionService = taskExecutionService;
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<String>>> getResultList(@AuthenticationPrincipal UserDetailsUser user) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		try {
			ResponseWrapper<List<String>> response = ResponseWrapper
					.SuccessResponse(taskExecutionService.getAvailableResults());
			return new ResponseEntity<>(response, headers, HttpStatus.OK);
		} catch (IOException e) {
			ResponseWrapper<List<String>> response = ResponseWrapper.FailureResponse(500,
					"Couldn't generate list of results.");
			return new ResponseEntity<>(response, headers, HttpStatus.OK);
		}
	}

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<String>> getResult(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam(value = "resultId") String resultId) {
		logger.info("/processor/getResult: " + resultId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		try {
			ResponseWrapper<String> response = ResponseWrapper
					.SuccessResponse(taskExecutionService.getResult(resultId));
			return new ResponseEntity<>(response, headers, HttpStatus.OK);
		} catch (IOException e) {
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(500,
					"Couldn't generate list of results.");
			return new ResponseEntity<>(response, headers, HttpStatus.OK);
		}
	}

	@RequestMapping(value = { "" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteResult(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("resultName") String resultName) {
		boolean success;
		try {
			success = taskExecutionService.deleteResult(resultName);
		} catch (IOException e) {
			success = false;
			logger.warn("Caught exception while trying to delete result " + resultName);
		}

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(success), HttpStatus.OK);
	}
}
