package tom.task.controller;

public class StandaloneTaskRequest {

	private TaskRequest taskRequest;
	private TaskRequest outputTaskRequest;

	public StandaloneTaskRequest() {
	}

	public StandaloneTaskRequest(TaskRequest taskRequest, TaskRequest outputTaskRequest) {
		this.taskRequest = taskRequest;
		this.outputTaskRequest = outputTaskRequest;
	}

	public TaskRequest getTaskRequest() {
		return taskRequest;
	}

	public void setTaskRequest(TaskRequest taskRequest) {
		this.taskRequest = taskRequest;
	}

	public TaskRequest getOutputTaskRequest() {
		return outputTaskRequest;
	}

	public void setOutputTaskRequest(TaskRequest outputTaskRequest) {
		this.outputTaskRequest = outputTaskRequest;
	}

}
