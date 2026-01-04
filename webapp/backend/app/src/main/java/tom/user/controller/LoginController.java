package tom.user.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import tom.ApiError;
import tom.ApiException;
import tom.config.MintyConfiguration;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.model.security.UserDetailsUser;
import tom.user.model.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserServiceInternal;

@RestController
@RequestMapping("/api/login")
public class LoginController {

	private static final Logger logger = LogManager.getLogger(LoginController.class);

	private final UserRepository userRepository;
	private final UserServiceInternal userService;
	private final MetadataService metadataService;
	private final MintyConfiguration properties;

	public LoginController(UserRepository userRepository, UserServiceInternal userService,
			MetadataService metadataService, MintyConfiguration properties) {
		this.userRepository = userRepository;
		this.userService = userService;
		this.metadataService = metadataService;
		this.properties = properties;
	}

	@GetMapping
	public ResponseWrapper<User> user(@AuthenticationPrincipal UserDetailsUser user, HttpServletRequest request)
			throws ApiException {

		if (user == null) {
			return ResponseWrapper.FailureResponse(HttpStatus.BAD_REQUEST.value(), List.of("No user provided."));
		}
		User result = userService.getUserFromId(user.getId()).get();

		Duration sessionTimeoutMinutes = properties.getConfig().session().timeout();
		HttpSession s = request.getSession(true);
		if (s != null) {
			s.setMaxInactiveInterval((int) sessionTimeoutMinutes.toSeconds());
		}

		// Use this opportunity to scrub any no-longer-valid user defaults from the user
		// object.
		Map<String, String> defaults = properties.getUserDefaults();

		boolean anythingRemoved = result.getDefaults().entrySet()
				.removeIf(entry -> !defaults.containsKey(entry.getKey()));

		try {
			if (anythingRemoved) {
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
