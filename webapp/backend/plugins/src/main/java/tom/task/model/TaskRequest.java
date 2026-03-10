package tom.task.model;

import java.util.Map;
import java.util.UUID;

public class TaskRequest {

	private String taskName;
	private String stepName;
	private UUID id;
	private boolean loggingActive;
	private Map<String, Object> configuration;
	private TaskLayout layout;

	public TaskRequest() {
		taskName = "";
		stepName = "";
		loggingActive = true;
		configuration = Map.of();
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public boolean isLoggingActive() {
		return loggingActive;
	}

	public void setLoggingActive(boolean loggingActive) {
		this.loggingActive = loggingActive;
	}

	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	public TaskLayout getLayout() {
		return layout;
	}

	public void setLayout(TaskLayout layout) {
		this.layout = layout;
	}

}
