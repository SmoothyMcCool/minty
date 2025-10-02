package tom.workflow.service;

import java.util.List;
import java.util.UUID;

import tom.api.UserId;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.ResultTemplate;
import tom.workflow.model.Workflow;

public interface WorkflowService {

	String executeWorkflow(UserId userId, WorkflowRequest request);

	Workflow createWorkflow(UserId userId, Workflow workflow);

	Workflow updateWorkflow(UserId userId, Workflow workflow);

	List<Workflow> listWorkflows(UserId userId);

	Workflow getWorkflow(UserId userId, UUID workflowId);

	void deleteWorkflow(UserId userId, UUID workflowId);

	boolean isAllowedToExecute(UUID workflowId, UserId userId);

	boolean isWorkflowOwned(UUID workflowId, UserId userId);

	String addorUpdateResultTemplate(ResultTemplate resultTemplate);

	ResultTemplate getResultTemplate(String templateName);

	List<String> listResultTemplates();

}
