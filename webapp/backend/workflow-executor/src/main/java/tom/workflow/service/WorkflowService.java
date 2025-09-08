package tom.workflow.service;

import java.util.List;
import java.util.UUID;

import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.ResultTemplate;
import tom.workflow.model.Workflow;

public interface WorkflowService {

	String executeWorkflow(UUID userId, WorkflowRequest request);

	Workflow createWorkflow(UUID userId, Workflow workflow);

	Workflow updateWorkflow(UUID userId, Workflow workflow);

	List<Workflow> listWorkflows(UUID userId);

	Workflow getWorkflow(UUID userId, UUID workflowId);

	void deleteWorkflow(UUID userId, UUID workflowId);

	boolean isAllowedToExecute(UUID workflowId, UUID userId);

	boolean isWorkflowOwned(UUID workflowId, UUID userId);

	String addorUpdateResultTemplate(ResultTemplate resultTemplate);

	ResultTemplate getResultTemplate(String templateName);

	List<String> listResultTemplates();

}
