package tom.workflow.tracking.service;

import java.util.List;
import java.util.UUID;

import tom.workflow.service.WorkflowRunner;
import tom.workflow.tracking.model.controller.WorkflowResult;
import tom.workflow.tracking.model.controller.WorkflowState;

public interface WorkflowTrackingService {

	void workflowCompleted(WorkflowRunner runner);

	void trackWorkflow(WorkflowRunner runner);

	String getOutput(UUID userId, UUID workflowId);

	void deleteResult(UUID userId, UUID workflowId);

	List<WorkflowState> getWorkflowList(UUID userId);

	WorkflowResult getResult(UUID userId, UUID workflowId);
}
