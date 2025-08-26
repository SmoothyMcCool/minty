package tom.output;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import tom.model.ChatMessage;

public class ExecutionResult {

	private Instant startTime;
	private Instant endTime;
	private List<List<Map<String, Object>>> results;
	private List<List<String>> errors;
	private List<ChatMessage> chatMessages = List.of();

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

	public List<List<Map<String, Object>>> getResults() {
		return results;
	}

	public void setResults(List<List<Map<String, Object>>> results) {
		this.results = results;
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

	public void setChatMessages(List<ChatMessage> chatMessages) {
		this.chatMessages = chatMessages;
	}

	public Map<String, Object> toMap() {
		return Map.of("startTime", startTime, "endTime", endTime, "results", results, "errors", errors, "chatMessages",
				chatMessages);
	}

}
