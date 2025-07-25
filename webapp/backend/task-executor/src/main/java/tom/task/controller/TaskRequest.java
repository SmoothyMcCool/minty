package tom.task.controller;

import java.util.Map;

public class TaskRequest {

	private String name = "";
	private Map<String, String> configuration = Map.of();

	public TaskRequest() {
	}

	public TaskRequest(String name, Map<String, String> configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

}
