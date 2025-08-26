package tom.workflow.tracking.model.controller;

import java.util.UUID;

import tom.workflow.tracking.model.ExecutionState;

public record WorkflowState(UUID id, String name, ExecutionState state) {

}
