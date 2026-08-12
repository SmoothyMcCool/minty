package tom.messages.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tom.controller.ResponseWrapper;
import tom.messages.service.MessagesService;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

	private final MessagesService service;

	public MessagesController(MessagesService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<ResponseWrapper<List<String>>> getMessages() {
		ResponseWrapper<List<String>> response = ResponseWrapper.SuccessResponse(service.getMessages());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/random")
	public ResponseEntity<ResponseWrapper<String>> getRandomMessage() {
		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse(service.getRandomMessage());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/motd")
	public ResponseEntity<ResponseWrapper<String>> getMessageOfTheDay() {
		ResponseWrapper<String> response = ResponseWrapper.SuccessResponse(service.getMessageOfTheDay());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}