package tom.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResponseWrapper<String>> handleException(Exception e) {
		logger.error("Caught unhandled exception: ", e);
		ResponseWrapper<String> response = ResponseWrapper.FailureResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went a bit squiffy. Sorry.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
