package tom.user;

import java.util.List;

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
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.user.repository.EncryptedUser;
import tom.user.repository.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserService;

@RestController
@RequestMapping("/api/login")
public class LoginController {

	private static final Logger logger = LogManager.getLogger(LoginController.class);

	private final UserRepository userRepository;
	private final UserService userService;

	public LoginController(UserRepository userRepository, UserService userService) {
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseWrapper<User> user(@AuthenticationPrincipal UserDetailsUser user, HttpServletRequest request)
			throws ApiException {

		if (user == null) {
			return ResponseWrapper.FailureResponse(HttpStatus.BAD_REQUEST.value(), List.of("No user provided."));
		}
		EncryptedUser _user = userRepository.findById(user.getId()).get();
		_user.setPassword("");

		// Set a 30-minute session timeout.
		HttpSession s = request.getSession(true);
		if (s != null) {
			s.setMaxInactiveInterval(30 * 60);
		}
		User result;
		try {
			result = userService.decrypt(_user);
			result.setCorpAccount("no");
			result.setCorpPassword("lol");
		} catch (JsonProcessingException e) {
			throw new ApiException(ApiError.FAILED_TO_DECRYPT_USER);
		}

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
