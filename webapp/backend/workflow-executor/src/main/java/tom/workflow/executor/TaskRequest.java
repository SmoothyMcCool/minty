package tom.workflow.executor;

import java.util.Map;
import java.util.UUID;

public class TaskRequest {

	private String taskName;
	private String stepName;
	private UUID id;
	private Map<String, String> configuration;
	private TaskLayout layout;

	public TaskRequest() {
		taskName = "";
		stepName = "";
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

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

	public TaskLayout getLayout() {
		return layout;
	}

	public void setLayout(TaskLayout layout) {
		this.layout = layout;
	}

}
