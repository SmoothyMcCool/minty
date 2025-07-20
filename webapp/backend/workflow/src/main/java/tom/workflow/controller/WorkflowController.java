package tom.workflow.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import tom.task.AiTask;
import tom.workflow.executor.service.WorkflowExecutionService;
import tom.workflow.filesystem.repository.FilesystemWatcher;
import tom.workflow.filesystem.repository.FilesystemWatcherRepository;
import tom.workflow.filesystem.repository.TriggeredWorkflowTask;
import tom.workflow.filesystem.service.FilesystemWatcherService;
import tom.workflow.repository.WorkflowTask;
import tom.workflow.repository.WorkflowTaskRepository;
import tom.workflow.taskregistry.TaskRegistryService;

@Controller
@RequestMapping("/api/workflow")
public class WorkflowController {

	private final Logger logger = LogManager.getLogger(WorkflowController.class);

	private final WorkflowExecutionService workflowExecutionService;
	private final WorkflowTaskRepository workflowTaskRepository;
	private final TaskRegistryService taskRegistry;
	private final FilesystemWatcherService filesystemWatcherService;
	private final FilesystemWatcherRepository filesystemWatcherRepository;
	private final MetadataService metadataService;

	public WorkflowController(WorkflowExecutionService workflowExecutionService,
			WorkflowTaskRepository workflowTaskRepository, TaskRegistryService taskRegistry,
			FilesystemWatcherService filesystemWatcherService, FilesystemWatcherRepository filesystemWatcherRepository,
			MetadataService metadataService) {
		this.workflowExecutionService = workflowExecutionService;
		this.workflowTaskRepository = workflowTaskRepository;
		this.taskRegistry = taskRegistry;
		this.filesystemWatcherService = filesystemWatcherService;
		this.filesystemWatcherRepository = filesystemWatcherRepository;
		this.metadataService = metadataService;
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<WorkflowTask>> newTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody WorkflowTask newTask) {
		WorkflowTask task = workflowTaskRepository.save(newTask);
		metadataService.newWorkflow(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task), HttpStatus.OK);
	}

	@RequestMapping(value = { "/trigger/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<FilesystemWatcher>> newTriggeredTask(
			@AuthenticationPrincipal UserDetailsUser user, @RequestBody TriggeredWorkflowTask newTask) {
		FilesystemWatcher watcher = filesystemWatcherService.newFilesystemWatcher(newTask);
		metadataService.newWorkflow(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(watcher), HttpStatus.OK);
	}

	@RequestMapping(value = { "/task" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<WorkflowTask>> getTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("taskId") int taskId) {
		WorkflowTask task = workflowTaskRepository.findById(taskId).get();

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task), HttpStatus.OK);
	}

	@RequestMapping(value = { "/task" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<List<WorkflowTask>>> deleteTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("taskId") int taskId) {
		WorkflowTask task = workflowTaskRepository.findById(taskId).get();
		workflowTaskRepository.delete(task);

		return listAllTasks(user);
	}

	@RequestMapping(value = { "/config" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getTaskConfig(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("taskId") int taskId) {
		WorkflowTask task = workflowTaskRepository.findById(taskId).get();
		Map<String, String> config = taskRegistry.getConfigFor(task.getWorkflow());

		if (config.isEmpty()) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.NOT_FOUND.value(), List.of(ApiError.TASK_NOT_FOUND)),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}

	@RequestMapping(value = { "/task/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<WorkflowTask>>> listAllTasks(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<WorkflowTask> tasks = new ArrayList<>();
		Iterable<WorkflowTask> iterable = workflowTaskRepository.findAll();
		iterable.forEach(tasks::add);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(tasks), HttpStatus.OK);
	}

	@RequestMapping(value = { "/task/trigger/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<FilesystemWatcher>>> listAllTriggeredTasks(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<FilesystemWatcher> tasks = new ArrayList<>();
		Iterable<FilesystemWatcher> iterable = filesystemWatcherRepository.findAll();
		iterable.forEach(tasks::add);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(tasks), HttpStatus.OK);
	}

	@RequestMapping(value = { "/workflows" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistry.getWorkflows()), HttpStatus.OK);
	}

	@RequestMapping(value = { "/execute" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<String>> executeTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody WorkflowRequest request) {
		logger.info("executeTask: " + request.getRequest());

		AiTask task = taskRegistry.newTask(user.getId(), request);

		if (task == null) {
			return new ResponseEntity<>(ResponseWrapper.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					List.of(ApiError.TASK_NOT_FOUND)), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		workflowExecutionService.executeTask(task);
		metadataService.workflowExecuted(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task.taskName()), HttpStatus.OK);
	}

	@RequestMapping(value = { "/resultList" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<String>>> getResultList() {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		try {
			ResponseWrapper<List<String>> response = ResponseWrapper
					.SuccessResponse(workflowExecutionService.getAvailableResults());
			return new ResponseEntity<>(response, headers, HttpStatus.OK);
		} catch (IOException e) {
			ResponseWrapper<List<String>> response = ResponseWrapper.FailureResponse(500,
					"Couldn't generate list of results.");
			return new ResponseEntity<>(response, headers, HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = { "/result" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<String>> getResult(@RequestParam(value = "resultId") String resultId) {
		logger.info("/processor/getResult: " + resultId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		try {
			ResponseWrapper<String> response = ResponseWrapper
					.SuccessResponse(workflowExecutionService.getResult(resultId));
			return new ResponseEntity<>(response, headers, HttpStatus.OK);
		} catch (IOException e) {
			ResponseWrapper<String> response = ResponseWrapper.FailureResponse(500,
					"Couldn't generate list of results.");
			return new ResponseEntity<>(response, headers, HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = { "/result" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<Boolean>> deleteResult(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("resultName") String resultName) {
		boolean success;
		try {
			success = workflowExecutionService.deleteResult(resultName);
		} catch (IOException e) {
			success = false;
			logger.warn("Caught exception while trying to delete result " + resultName);
		}

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(success), HttpStatus.OK);
	}
}
