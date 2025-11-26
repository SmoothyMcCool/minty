package tom.user.controller;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import tom.ApiError;
import tom.ApiException;
import tom.config.ExternalProperties;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;
import tom.user.model.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserServiceInternal;
import tom.workflow.taskregistry.TaskRegistryService;

@RestController
@RequestMapping("/api/login")
public class LoginController {

	private static final Logger logger = LogManager.getLogger(LoginController.class);

	private final UserRepository userRepository;
	private final UserServiceInternal userService;
	private final MetadataService metadataService;
	private final TaskRegistryService taskRegistryService;
	private final ExternalProperties properties;

	public LoginController(UserRepository userRepository, UserServiceInternal userService,
			MetadataService metadataService, TaskRegistryService taskRegistryService, ExternalProperties properties) {
		this.userRepository = userRepository;
		this.userService = userService;
		this.metadataService = metadataService;
		this.taskRegistryService = taskRegistryService;
		this.properties = properties;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseWrapper<User> user(@AuthenticationPrincipal UserDetailsUser user, HttpServletRequest request)
			throws ApiException {

		if (user == null) {
			return ResponseWrapper.FailureResponse(HttpStatus.BAD_REQUEST.value(), List.of("No user provided."));
		}
		User result = userService.getUserFromId(user.getId()).get();

		int sessionTimeoutMinutes = properties.getInt("session.timeout", 30);
		HttpSession s = request.getSession(true);
		if (s != null) {
			s.setMaxInactiveInterval(sessionTimeoutMinutes * 60);
		}

		// Use this opportunity to scrub any no-longer-valid user defaults from the user
		// object.
		Boolean updateUser = false;
		Map<String, String> defaults = taskRegistryService.getUserDefaults();
		for (Map.Entry<String, String> entry : result.getDefaults().entrySet()) {
			if (!defaults.containsKey(entry.getKey())) {
				result.getDefaults().remove(entry.getKey());
				updateUser = true;
			}
		}
		try {
			if (updateUser) {
				userRepository.save(userService.encrypt(result));
			}
		} catch (JsonProcessingException e) {
			throw new ApiException(ApiError.FAILED_TO_DECRYPT_USER);
		}

		metadataService.userLoggedIn(user.getId());

		result.setPassword("");

		return ResponseWrapper.SuccessResponse(result);
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ResponseWrapper<String>> badUserExceptionHandler(HttpServletRequest req, ApiException e) {
		logger.error("LoginController: Caught ApiException: ", e.getMessage());
		ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
				e.getApiErrors());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
}
