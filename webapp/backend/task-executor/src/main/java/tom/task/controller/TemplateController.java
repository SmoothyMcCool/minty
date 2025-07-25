package tom.task.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import tom.ApiError;
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.task.model.StandaloneTask;
import tom.task.repository.StandaloneTaskRepository;
import tom.task.taskregistry.TaskRegistryService;

@Controller
@RequestMapping("/api/task/template")
public class TemplateController {

	private final StandaloneTaskRepository taskRepository;
	private final TaskRegistryService taskRegistryService;

	public TemplateController(StandaloneTaskRepository taskRepository, TaskRegistryService taskRegistryService) {
		this.taskRepository = taskRepository;
		this.taskRegistryService = taskRegistryService;
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listTaskTemplates(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistryService.getTasks()), HttpStatus.OK);
	}

	@RequestMapping(value = { "/output/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listOutputTaskTemplates(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistryService.getOutputTaskTemplates()),
				HttpStatus.OK);
	}

	@RequestMapping(value = { "/config" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getTaskConfiguration(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("taskId") int taskId) {
		StandaloneTask task = taskRepository.findById(taskId).get();
		Map<String, String> config = taskRegistryService.getConfigForTask(task.getTaskTemplate().getName());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}

	@RequestMapping(value = { "/output/config" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, String>>> getOutputTaskConfiguration(
			@AuthenticationPrincipal UserDetailsUser user, @RequestParam("taskId") int taskId) {
		StandaloneTask task = taskRepository.findById(taskId).get();
		Map<String, String> config = taskRegistryService.getConfigForOutputTask(task.getOutputTemplate().getName());
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}

	@RequestMapping(value = { "/config/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listTaskConfigurations(
			@AuthenticationPrincipal UserDetailsUser user) {
		Map<String, Map<String, String>> config = taskRegistryService.listTaskConfigurations();

		if (config.isEmpty()) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.NOT_FOUND.value(), List.of(ApiError.NOT_FOUND)),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}

	@RequestMapping(value = { "/output/config/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<Map<String, Map<String, String>>>> listOutputTaskConfigurations(
			@AuthenticationPrincipal UserDetailsUser user) {
		Map<String, Map<String, String>> config = taskRegistryService.listOutputTaskConfigurations();

		if (config.isEmpty()) {
			return new ResponseEntity<>(
					ResponseWrapper.ApiFailureResponse(HttpStatus.NOT_FOUND.value(), List.of(ApiError.NOT_FOUND)),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(config), HttpStatus.OK);
	}
}
