package tom.task.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.executor.service.TaskExecutionService;
import tom.task.model.StandaloneTask;
import tom.task.repository.StandaloneTaskRepository;
import tom.task.taskregistry.TaskRegistryService;

@Controller
@RequestMapping("/api/task")
public class TaskController {

	private final Logger logger = LogManager.getLogger(TaskController.class);

	private final TaskExecutionService taskExecutionService;
	private final StandaloneTaskRepository taskRepository;
	private final TaskRegistryService taskRegistryService;
	private final MetadataService metadataService;

	public TaskController(TaskExecutionService taskExecutionService, StandaloneTaskRepository taskRepository,
			TaskRegistryService taskRegistryService, MetadataService metadataService) {
		this.taskExecutionService = taskExecutionService;
		this.taskRepository = taskRepository;
		this.taskRegistryService = taskRegistryService;
		this.metadataService = metadataService;
	}

	@RequestMapping(value = { "/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<StandaloneTask>> newTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody StandaloneTask newTask) {
		StandaloneTask task = taskRepository.save(newTask);
		metadataService.taskCreated(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task), HttpStatus.OK);
	}

	@RequestMapping(value = { "/triggered/new" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<StandaloneTask>> newTriggeredTask(
			@AuthenticationPrincipal UserDetailsUser user, @RequestBody StandaloneTask newTask) {
		// Make sure triggered is set.
		newTask.setTriggered(true);
		return newTask(user, newTask);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<StandaloneTask>> getTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestParam("taskId") int taskId) {
		StandaloneTask task = taskRepository.findById(taskId).get();

		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task), HttpStatus.OK);
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<StandaloneTask>>> listAllTasks(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<StandaloneTask> tasks = new ArrayList<>();
		Iterable<StandaloneTask> iterable = taskRepository.findAll();
		iterable.forEach(tasks::add);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(tasks), HttpStatus.OK);
	}

	@RequestMapping(value = { "/triggered/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<StandaloneTask>>> listAllTriggeredTasks(
			@AuthenticationPrincipal UserDetailsUser user) {
		List<StandaloneTask> tasks = new ArrayList<>();
		Iterable<StandaloneTask> iterable = taskRepository.findAllByTriggeredTrue();
		iterable.forEach(tasks::add);
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(tasks), HttpStatus.OK);
	}

	@RequestMapping(value = { "" }, method = RequestMethod.DELETE)
	public ResponseEntity<ResponseWrapper<List<StandaloneTask>>> deleteTask(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("taskId") int taskId) {
		StandaloneTask task = taskRepository.findById(taskId).get();
		taskRepository.delete(task);

		return listAllTasks(user);
	}

	@RequestMapping(value = { "/execute" }, method = RequestMethod.POST)
	public ResponseEntity<ResponseWrapper<String>> executeTask(@AuthenticationPrincipal UserDetailsUser user,
			@RequestBody StandaloneTaskRequest request) {
		logger.info("executeTask: " + request.getTaskRequest().getName());

		AiTask task = taskRegistryService.newTask(user.getId(), request.getTaskRequest());

		OutputTask outputTask = taskRegistryService.newOutputTask(0, request.getOutputTaskRequest());

		if (task == null) {
			return new ResponseEntity<>(ResponseWrapper.ApiFailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					List.of(ApiError.NOT_FOUND)), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		taskExecutionService.executeTask(task, outputTask);
		metadataService.taskExecuted(user.getId());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(task.taskName()), HttpStatus.OK);
	}
}
