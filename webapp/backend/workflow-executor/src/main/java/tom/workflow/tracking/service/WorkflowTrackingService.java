package tom.workflow.tracking.service;

import java.util.List;
import java.util.UUID;

import tom.api.UserId;
import tom.workflow.executor.WorkflowRunner;
import tom.workflow.tracking.controller.model.WorkflowResult;
import tom.workflow.tracking.controller.model.WorkflowState;

public interface WorkflowTrackingService {

	void workflowCompleted(WorkflowRunner runner);

	void trackWorkflow(WorkflowRunner runner);

	String getOutput(UserId userId, UUID workflowId);

	void deleteResult(UserId userId, UUID workflowId);

	List<WorkflowState> getWorkflowList(UserId userId);

	WorkflowResult getResult(UserId userId, UUID workflowId);

	String getLog(UserId userId, UUID workflowId);

	void cancelWorkflow(UserId userId, String name);
}
