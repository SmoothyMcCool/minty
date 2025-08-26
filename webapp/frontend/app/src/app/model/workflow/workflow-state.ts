import { WorkflowExecutionState } from "./workflow-execution-state";

export interface WorkflowState {
	id: string;
	name: string;
	state: WorkflowExecutionState;
}