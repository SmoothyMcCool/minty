package tom.task.controller;

import java.util.Map;

public class TaskRequest {

	private String request = "";
	private Map<String, String> data = Map.of();
	private String outputTask = "";
	private Map<String, String> outputTaskConfig = Map.of();

	public TaskRequest() {

	}

	public TaskRequest(String request, Map<String, String> data) {
		this.request = request;
		this.data = data;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public Map<String, String> getData() {
		return data;
	}

	public void setData(Map<String, String> data) {
		this.data = data;
	}

	public String getOutputTask() {
		return outputTask;
	}

	public void setOutputTask(String outputTask) {
		this.outputTask = outputTask;
	}

	public Map<String, String> getOutputTaskConfig() {
		return outputTaskConfig;
	}

	public void setOutputTaskConfig(Map<String, String> outputTaskConfig) {
		this.outputTaskConfig = outputTaskConfig;
	}

}
