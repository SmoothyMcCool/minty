package tom.workflow.tracking.service;

import java.util.List;
import java.util.UUID;

import tom.api.UserId;
import tom.workflow.service.WorkflowRunner;
import tom.workflow.tracking.model.controller.WorkflowResult;
import tom.workflow.tracking.model.controller.WorkflowState;

public interface WorkflowTrackingService {

	void workflowCompleted(WorkflowRunner runner);

	void trackWorkflow(WorkflowRunner runner);

	String getOutput(UserId userId, UUID workflowId);

	void deleteResult(UserId userId, UUID workflowId);

	List<WorkflowState> getWorkflowList(UserId userId);

	WorkflowResult getResult(UserId userId, UUID workflowId);
}
