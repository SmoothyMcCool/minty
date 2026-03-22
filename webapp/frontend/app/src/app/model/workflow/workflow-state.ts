import { WorkflowExecutionState } from './workflow-execution-state';

export interface WorkflowState {
	id: string;
	name: string;
	failed: boolean;
	state: WorkflowExecutionState;
}