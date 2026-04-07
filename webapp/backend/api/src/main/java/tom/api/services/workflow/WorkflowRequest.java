package tom.api.services.workflow;

import java.util.List;
import java.util.Map;

import tom.api.WorkflowId;

public class WorkflowRequest {

	private WorkflowId id;
	private String logLevel;
	private List<Map<String, Object>> taskConfigurationList = List.of();
	private Map<String, Object> outputConfiguration = Map.of();

	public WorkflowRequest() {
	}

	public WorkflowRequest(WorkflowId id, String logLevel, List<Map<String, Object>> taskConfigurationList,
			Map<String, Object> outputConfiguration) {
		this.id = id;
		this.logLevel = logLevel;
		this.taskConfigurationList = taskConfigurationList;
		this.outputConfiguration = outputConfiguration;
	}

	public WorkflowId getId() {
		return id;
	}

	public void setId(WorkflowId id) {
		this.id = id;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
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
