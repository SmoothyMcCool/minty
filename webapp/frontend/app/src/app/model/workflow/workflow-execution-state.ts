import { WorkflowExecutionStepState } from "./workflow-execution-step-state";

export interface WorkflowExecutionState {
	stepStates: WorkflowExecutionStepState[];
}