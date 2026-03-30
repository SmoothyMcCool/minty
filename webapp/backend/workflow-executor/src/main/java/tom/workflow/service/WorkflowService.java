package tom.workflow.service;

import java.util.List;
import java.util.UUID;

import tom.api.UserId;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.user.model.ResourceSharingSelection;
import tom.user.model.UserSelection;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.Workflow;

public interface WorkflowService {

	String executeWorkflow(UserId userId, WorkflowRequest request);

	Workflow createWorkflow(UserId userId, Workflow workflow) throws NotOwnedException;

	Workflow updateWorkflow(UserId userId, Workflow workflow);

	List<Workflow> listWorkflows(UserId userId);

	Workflow getWorkflow(UserId userId, UUID workflowId);

	void deleteWorkflow(UserId userId, UUID workflowId);

	boolean isAllowedToExecute(UUID workflowId, UserId userId);

	boolean isWorkflowOwned(UUID workflowId, UserId userId);

	boolean isRunningWorkflowOwned(UserId userId, String name);

	void cancelWorkflow(UserId userId, String name) throws NotOwnedException;

	void shareWorkflow(UserId userId, ResourceSharingSelection selection) throws NotFoundException;

	UserSelection getSharingFor(UserId userId, String name) throws NotOwnedException, NotFoundException;

}
