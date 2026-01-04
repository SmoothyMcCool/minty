package tom.workflow.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkflowRequest {

	private UUID id;
	private List<Map<String, Object>> taskConfigurationList = List.of();
	private Map<String, Object> outputConfiguration = Map.of();

	public WorkflowRequest() {
	}

	public WorkflowRequest(UUID id, List<Map<String, Object>> taskConfigurationList,
			Map<String, Object> outputConfiguration) {
		this.id = id;
		this.taskConfigurationList = taskConfigurationList;
		this.outputConfiguration = outputConfiguration;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public List<Map<String, Object>> getTaskConfigurationList() {
		return taskConfigurationList;
	}

	public void setTaskConfigurationList(List<Map<String, Object>> taskConfigurationList) {
		this.taskConfigurationList = taskConfigurationList;
	}

	public Map<String, Object> getOutputConfiguration() {
		return outputConfiguration;
	}

	public void setOutputConfiguration(Map<String, Object> outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

}
