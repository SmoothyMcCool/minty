package tom.workflow.tracking.controller.model;

import java.util.UUID;

import tom.api.task.ExecutionResult;

public record WorkflowResult(UUID id, String name, ExecutionResult result, String output,
		String outputFormat) {

}
