import { WorkflowExecutionResult } from "./workflow-execution-result";

export interface WorkflowResult {
	id: string;
	name: string;
	result: WorkflowExecutionResult;
	output: string;
	outputFormat: string
}