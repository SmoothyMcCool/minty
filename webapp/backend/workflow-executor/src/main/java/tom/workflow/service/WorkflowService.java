package tom.workflow.service;

import java.util.List;

import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.Workflow;

public interface WorkflowService {

	void executeWorkflow(int userId, WorkflowRequest request);

	Workflow createWorkflow(int userId, Workflow workflow);

	List<Workflow> listWorkflows(int userId);

	Workflow getWorkflow(int userId, int workflowId);

	void deleteWorkflow(int userId, int workflowId);

}
