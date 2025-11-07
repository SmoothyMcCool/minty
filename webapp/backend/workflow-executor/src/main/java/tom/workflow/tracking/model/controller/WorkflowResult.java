package tom.workflow.tracking.model.controller;

import java.util.UUID;

import tom.task.ExecutionResult;

public record WorkflowResult(UUID id, String name, ExecutionResult result, String output,
		String outputFormat) {

}
