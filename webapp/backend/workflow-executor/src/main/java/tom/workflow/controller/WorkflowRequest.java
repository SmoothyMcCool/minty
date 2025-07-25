package tom.workflow.controller;

import java.util.List;
import java.util.Map;

public class WorkflowRequest {

	private int id = 0;
	private List<Map<String, String>> taskConfigurationList = List.of();
	private Map<String, String> outputConfiguration = Map.of();

	public WorkflowRequest() {
	}

	public WorkflowRequest(int id, List<Map<String, String>> taskConfigurationList,
			Map<String, String> outputConfiguration) {
		this.id = id;
		this.taskConfigurationList = taskConfigurationList;
		this.outputConfiguration = outputConfiguration;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Map<String, String>> getTaskConfigurationList() {
		return taskConfigurationList;
	}

	public void setTaskConfigurationList(List<Map<String, String>> taskConfigurationList) {
		this.taskConfigurationList = taskConfigurationList;
	}

	public Map<String, String> getOutputConfiguration() {
		return outputConfiguration;
	}

	public void setOutputConfiguration(Map<String, String> outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

}
