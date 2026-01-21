package tom.api.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

public class ExecutionResult {

	private Instant startTime;
	private Instant endTime;
	private String logFile;
	private Map<String, List<Packet>> results;
	private Map<String, List<String>> errors;

	public ExecutionResult() {
		startTime = Instant.now();
		endTime = Instant.now();
		logFile = "";
		results = new ConcurrentHashMap<>();
		errors = new ConcurrentHashMap<>();
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

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public Map<String, List<Packet>> getResults() {
		return results;
	}

	public void setResults(Map<String, List<Packet>> results) {
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

	public void addResult(String stepName, Packet result) {
		results.get(stepName).add(result);
	}

	public void addError(String stepName, String error) {
		if (StringUtils.isNotBlank(error)) {
			errors.get(stepName).add(error);
		}
	}

	public void start() {
		setStartTime(Instant.now());
	}

	public void stop() {
		setEndTime(Instant.now());
	}

	public tom.api.task.ExecutionResult toApiResult() {
		tom.api.task.ExecutionResult result = new tom.api.task.ExecutionResult();
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
