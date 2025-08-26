package tom.workflow.tracking.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tom.model.ChatMessage;

public class ExecutionResult {

	private Instant startTime;
	private Instant endTime;
	private List<List<Map<String, Object>>> results;
	private List<List<String>> errors;
	private List<ChatMessage> chatMessages = List.of();

	public ExecutionResult() {
		results = new ArrayList<>();
		errors = new ArrayList<>();
	}

	public ExecutionResult(int numSteps) {
		results = new ArrayList<>();
		errors = new ArrayList<>();

		for (int i = 0; i < numSteps; i++) {
			results.add(new ArrayList<>());
			errors.add(new ArrayList<>());
		}
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public List<List<String>> getErrors() {
		return errors;
	}

	public void setErrors(List<List<String>> errors) {
		this.errors = errors;
	}

	public List<ChatMessage> getChatMessages() {
		return chatMessages;
	}

	public List<List<Map<String, Object>>> getResults() {
		return results;
	}

	public void setResults(List<List<Map<String, Object>>> results) {
		this.results = results;
	}

	public void addResult(int step, Map<String, Object> result) {
		results.get(step).add(result);
	}

	public void addError(int step, String error) {
		if (error != null && !error.isBlank()) {
			errors.get(step).add(error);
		}
	}

	public void start() {
		startTime = Instant.now();
	}

	public void stop() {
		endTime = Instant.now();
	}

	public void setChatMessages(List<ChatMessage> chatMessages) {
		this.chatMessages = chatMessages;
	}

	public tom.output.ExecutionResult toApiResult() {
		tom.output.ExecutionResult result = new tom.output.ExecutionResult();
		result.setChatMessages(chatMessages);
		result.setEndTime(endTime);
		result.setErrors(errors);
		result.setResults(results);
		result.setStartTime(startTime);

		return result;
	}

}
