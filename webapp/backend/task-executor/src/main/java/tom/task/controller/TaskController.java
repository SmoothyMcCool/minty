package tom.task.controller;

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
import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.executor.service.TaskExecutionService;
import tom.task.filesystem.repository.FilesystemWatcher;
import tom.task.filesystem.repository.FilesystemWatcherRepository;
import tom.task.filesystem.repository.TriggeredTask;
import tom.task.filesystem.service.FilesystemWatcherService;
import tom.task.model.Task;
import tom.task.repository.TaskRepository;
import tom.task.taskregistry.TaskRegistryService;

@Controller
@RequestMapping("/api/task")
public class TaskController {

	private final Logger logger = LogManager.getLogger(TaskController.class);

	private final TaskExecutionService taskExecutionService;
	private final TaskRepository taskRepository;
	private final TaskRegistryService taskRegistryService;
	private final FilesystemWatcherService filesystemWatcherService;
	private final FilesystemWatcherRepository filesystemWatcherRepository;
	private final MetadataService metadataService;

	public TaskController(TaskExecutionService taskExecutionService, TaskRepository taskRepository,
			TaskRegistryService taskRegistryService, FilesystemWatcherService filesystemWatcherService,
			FilesystemWatcherRepository filesystemWatcherRepository, MetadataService metadataService) {
		this.taskExecutionService = taskExecutionService;
		this.taskRepository = taskRepository;
		this.taskRegistryService = taskRegistryService;
		this.filesystemWatcherService = filesystemWatcherService;
		this.filesystemWatcherRepository = filesystemWatcherRepository;
		this.metadataService = metadataService;
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<Task>> newTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody Task newTask) {
		Task task = taskRepository.save(newTask);
		metadataService.taskCreated(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task), HttpStatus.OK);
	}

	@RequestMapping(value = { "/triggered/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<FilesystemWatcher>> newTriggeredTask(
			@AuthenticationPrincipal UserDetailsUser user, @RequestBody TriggeredTask newTask) {
		FilesystemWatcher watcher = filesystemWatcherService.newFilesystemWatcher(newTask);
		metadataService.taskCreated(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(watcher), HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Task>> getTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("taskId") int taskId) {
		Task task = taskRepository.findById(taskId).get();

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task), HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<List<Task>>> deleteTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("taskId") int taskId) {
		Task task = taskRepository.findById(taskId).get();
		taskRepository.delete(task);

		return listAllTasks(user);
	}

	@RequestMapping(value = { "/templates/config" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getTaskConfig(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("taskId") int taskId) {
		Task task = taskRepository.findById(taskId).get();
		Map<String, String> config = taskRegistryService.getConfigForTask(task.getTemplate());

		if (config.isEmpty()) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.NOT_FOUND.value(), List.of(ApiError.TASK_NOT_FOUND)),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}

	@RequestMapping(value = { "/output/config" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getOutputTaskConfig(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("taskId") int taskId) {
		Task task = taskRepository.findById(taskId).get();
		Map<String, String> config = taskRegistryService.getConfigForOutputTask(task.getOutputTask());

		if (config.isEmpty()) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.NOT_FOUND.value(), List.of(ApiError.TASK_NOT_FOUND)),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<Task>>> listAllTasks(@AuthenticationPrincipal UserDetailsUser user) {
		List<Task> tasks = new ArrayList<>();
		Iterable<Task> iterable = taskRepository.findAll();
		iterable.forEach(tasks::add);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(tasks), HttpStatus.OK);
	}

	@RequestMapping(value = { "/triggered/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<FilesystemWatcher>>> listAllTriggeredTasks(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<FilesystemWatcher> tasks = new ArrayList<>();
		Iterable<FilesystemWatcher> iterable = filesystemWatcherRepository.findAll();
		iterable.forEach(tasks::add);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(tasks), HttpStatus.OK);
	}

	@RequestMapping(value = { "/templates" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listWorkflows(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistryService.getTasks()), HttpStatus.OK);
	}

	@RequestMapping(value = { "/output" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listRenderers(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistryService.getOutputTasks()),
				HttpStatus.OK);
	}

	@RequestMapping(value = { "/execute" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<String>> executeTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody TaskRequest request) {
		logger.info("executeTask: " + request.getRequest());

		AiTask task = taskRegistryService.newTask(user.getId(), request);

		OutputTask outputTask = taskRegistryService.newOutputTask(0, request);

		if (task == null) {
			return new ResponseEntity<>(ResponseWrapper.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					List.of(ApiError.TASK_NOT_FOUND)), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		taskExecutionService.executeTask(task, outputTask);
		metadataService.taskExecuted(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task.taskName()), HttpStatus.OK);
	}

	@RequestMapping(value = { "/resultList" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<String>>> getResultList() {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		try {
			ResponseWrapper<List<String>> response = ResponseWrapper
					.SuccessResponse(taskExecutionService.getAvailableResults());
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
					.SuccessResponse(taskExecutionService.getResult(resultId));
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
			success = taskExecutionService.deleteResult(resultName);
		} catch (IOException e) {
			success = false;
			logger.warn("Caught exception while trying to delete result " + resultName);
		}

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(success), HttpStatus.OK);
	}
}
