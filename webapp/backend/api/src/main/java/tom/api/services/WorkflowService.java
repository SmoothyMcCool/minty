package tom.api.services;

import java.util.List;

import tom.api.UserId;
import tom.api.WorkflowId;
import tom.api.model.user.ResourceSharingSelection;
import tom.api.model.user.UserSelection;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.api.services.workflow.Workflow;
import tom.api.services.workflow.WorkflowDescription;
import tom.api.services.workflow.WorkflowRequest;

public interface WorkflowService {

	String executeWorkflow(UserId userId, WorkflowRequest request) throws NotOwnedException;

	Workflow createWorkflow(UserId userId, Workflow workflow) throws NotOwnedException;

	Workflow updateWorkflow(UserId userId, Workflow workflow) throws NotOwnedException;

	List<WorkflowDescription> listWorkflows(UserId userId);

	Workflow getWorkflow(UserId userId, WorkflowId workflowId);

	void deleteWorkflow(UserId userId, WorkflowId workflowId) throws NotOwnedException;

	void cancelWorkflow(UserId userId, String name) throws NotOwnedException;

	void shareWorkflow(UserId userId, ResourceSharingSelection selection) throws NotFoundException, NotOwnedException;

	UserSelection getSharingFor(UserId userId, String name) throws NotOwnedException, NotFoundException;

}
