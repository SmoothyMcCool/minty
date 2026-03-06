package tom.api.task;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

public class ExecutionResult {

	private Instant startTime;
	private Instant endTime;
	private String runtime;
	private String logFile;
	private String name;
	private Map<String, List<Packet>> results;
	private Map<String, List<String>> errors;

	public ExecutionResult() {
		startTime = Instant.now();
		endTime = Instant.now();
		logFile = "";
		name = "";
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
		calculateRuntime();
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
		calculateRuntime();
	}

	public void setRuntime(String runtime) {
		this.runtime = runtime;
	}

	public String getRuntime() {
		return runtime;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		return Map.of("name", name, "startTime", startTime, "endTime", endTime, "runtime", runtime, "results", results,
				"errors", errors);
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

	public void addStep(String stepName) {
		results.put(stepName, new ArrayList<>());
		errors.put(stepName, new ArrayList<>());
	}

	private void calculateRuntime() {
		Duration duration = java.time.Duration.between(startTime, endTime);
		runtime = String.format("%dh %dm %ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
}
