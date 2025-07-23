package tom.workflow.controller;

import java.util.List;
import java.util.Map;

public class WorkflowRequest {

	private int workflowId = 0;
	private List<Map<String, String>> taskConfigurationList = List.of();

	public WorkflowRequest(int workflowId, List<Map<String, String>> taskConfigurationList) {
		this.workflowId = workflowId;
		this.taskConfigurationList = taskConfigurationList;
	}

	public int getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(int workflowId) {
		this.workflowId = workflowId;
	}

	public List<Map<String, String>> getTaskConfigurationList() {
		return taskConfigurationList;
	}

	public void setTaskConfigurationList(List<Map<String, String>> taskConfigurationList) {
		this.taskConfigurationList = taskConfigurationList;
	}

}
