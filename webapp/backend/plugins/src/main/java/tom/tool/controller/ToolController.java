package tom.tool.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import tom.controller.ResponseWrapper;
import tom.model.security.UserDetailsUser;
import tom.tool.model.MintyToolDescription;
import tom.tool.registry.ToolRegistryService;

@Controller
@RequestMapping("/api/tools")
public class ToolController {

	private final ToolRegistryService toolRegistryService;

	public ToolController(ToolRegistryService toolRegistryService) {
		this.toolRegistryService = toolRegistryService;
	}

	@GetMapping(value = { "" })
	public ResponseEntity<ResponseWrapper<List<MintyToolDescription>>> listTools(
			@AuthenticationPrincipal UserDetailsUser user) {

		List<MintyToolDescription> tools = toolRegistryService.listTools();

		ResponseWrapper<List<MintyToolDescription>> response = ResponseWrapper.SuccessResponse(tools);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
