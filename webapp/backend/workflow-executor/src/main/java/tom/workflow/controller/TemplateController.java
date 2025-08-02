package tom.workflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.task.taskregistry.TaskRegistryService;
import tom.workflow.model.TaskDescription;

@Controller
@RequestMapping("/api/task/template")
public class TemplateController {

	private final TaskRegistryService taskRegistryService;

	public TemplateController(TaskRegistryService taskRegistryService) {
		this.taskRegistryService = taskRegistryService;
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<TaskDescription>>> listTaskTemplates(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistryService.getTasks()), HttpStatus.OK);
	}

	@RequestMapping(value = { "/output/list" }, method = RequestMethod.GET)
	public ResponseEntity<ResponseWrapper<List<TaskDescription>>> listOutputTaskTemplates(
			@AuthenticationPrincipal UserDetailsUser user) {
		return new ResponseEntity<>(ResponseWrapper.SuccessResponse(taskRegistryService.getOutputTaskTemplates()),
				HttpStatus.OK);
	}

}