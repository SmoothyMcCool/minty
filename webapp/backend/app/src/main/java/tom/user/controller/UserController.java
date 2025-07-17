package tom.user.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import tom.ApiError;
import tom.ApiException;
import tom.config.security.UserDetailsUser;
import tom.controller.ResponseWrapper;
import tom.meta.service.MetadataService;
import tom.user.repository.EncryptedUser;
import tom.user.repository.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final MetadataService metadataService;

    public UserController(UserRepository userRepository, UserService userService, MetadataService metadataService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.metadataService = metadataService;
    }

    @RequestMapping(value = { "/new" }, method = RequestMethod.POST)
    public ResponseEntity<ResponseWrapper<User>> newUser(@AuthenticationPrincipal UserDetailsUser userDetails,
            @RequestBody User user, HttpServletRequest request, BindingResult result) throws ApiException {

        if (result.hasErrors()) {
            throw new ApiException(ApiError.USER_OBJECT_CONTAINS_ERRORS);
        }

        logger.info(user.toString());

        List<ApiError> errors = new ArrayList<>();

        // Make sure the username isn't in use.
        if (userRepository.findByAccount(user.getName()) != null) {
            errors.add(ApiError.USERNAME_ALREADY_TAKEN);
        }

        // If above validation caught any errors, throw them.
        if (!errors.isEmpty()) {
            throw new ApiException(errors);
        }

        // Create the user.
        String hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hash);
        try {
            user = userService.decrypt(userRepository.save(userService.encrypt(user)));
        } catch (JsonProcessingException e) {
            errors.add(ApiError.FAILED_TO_DECRYPT_USER);
            throw new ApiException(errors);
        }

        // Automatically try to log in.
        try {
            request.login(user.getName(), user.getPassword());
        } catch (ServletException e) {
            logger.error("SignupController - exception while logging in a new user.");
        }

        user.setPassword("");

        metadataService.addUser(user.getId());

        ResponseWrapper<User> response = ResponseWrapper.SuccessResponse(user);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = { "/update" }, method = RequestMethod.POST)
    public ResponseEntity<ResponseWrapper<User>> updateUser(@AuthenticationPrincipal UserDetailsUser userDetails,
            @RequestBody User user, HttpServletRequest request, BindingResult result) throws ApiException {

        if (result.hasErrors()) {
            throw new ApiException(ApiError.USER_OBJECT_CONTAINS_ERRORS);
        }

        logger.info(user.toString());

        List<ApiError> errors = new ArrayList<>();

        // Make sure user is only trying to update themselves.
        if (userDetails.getId() != user.getId()) {
            errors.add(ApiError.CANT_UPDATE_OTHER_USERS);
        }
        // Make sure the username isn't in use.
        EncryptedUser existingUser = userRepository.findByAccount(user.getName());
        if (existingUser != null) {
            if (existingUser.getId() != user.getId()) {
                errors.add(ApiError.USERNAME_ALREADY_TAKEN);
            }
        }

        // If above validation caught any errors, throw them.
        if (!errors.isEmpty()) {
            throw new ApiException(errors);
        }

        // Update the user.
        String hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hash);
        try {
            EncryptedUser updatedUser = userService.encrypt(user);
            user = userService.decrypt(userRepository.save(updatedUser));
        } catch (JsonProcessingException e) {
            errors.add(ApiError.FAILED_TO_DECRYPT_USER);
            throw new ApiException(errors);
        }

        user.setPassword("");
        ResponseWrapper<User> response = ResponseWrapper.SuccessResponse(user);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResponseWrapper<String>> badUserExceptionHandler(HttpServletRequest req, ApiException e) {
        logger.error("UserController: Caught ApiException: ", e.getMessage());
        ResponseWrapper<String> response = ResponseWrapper.ApiFailureResponse(HttpStatus.BAD_REQUEST.value(),
                e.getApiErrors());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
