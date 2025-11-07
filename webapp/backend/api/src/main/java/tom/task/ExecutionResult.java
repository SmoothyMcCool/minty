package tom.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionResult {

	private Instant startTime;
	private Instant endTime;
	private Map<String, List<Map<String, Object>>> results;
	private Map<String, List<String>> errors;

	public ExecutionResult() {
		startTime = Instant.now();
		endTime = Instant.now();
		results = new HashMap<>();
		errors = new HashMap<>();
	}

	public ExecutionResult(List<String> stepNames) {
		this();

		for (String step : stepNames) {
			results.put(step, new ArrayList<>());
			errors.put(step, new ArrayList<>());
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

	public Map<String, List<Map<String, Object>>> getResults() {
		return results;
	}

	public void setResults(Map<String, List<Map<String, Object>>> results) {
		this.results = results;
	}

	public Map<String, List<String>> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, List<String>> errors) {
		this.errors = errors;
	}

	public Map<String, Object> toMap() {
		return Map.of("startTime", startTime, "endTime", endTime, "results", results, "errors", errors);
	}

	public void addResult(String stepName, Map<String, Object> result) {
		results.get(stepName).add(result);
	}

	public void addError(String stepName, String error) {
		if (error != null && !error.isBlank()) {
			errors.get(stepName).add(error);
		}
	}

	public void start() {
		setStartTime(Instant.now());
	}

	public void stop() {
		setEndTime(Instant.now());
	}

	public tom.task.ExecutionResult toApiResult() {
		tom.task.ExecutionResult result = new tom.task.ExecutionResult();
		result.setEndTime(endTime);
		result.setErrors(errors);
		result.setResults(results);
		result.setStartTime(startTime);

		return result;
	}

	public void addStep(String stepName) {
		results.put(stepName, new ArrayList<>());
		errors.put(stepName, new ArrayList<>());
	}
}
