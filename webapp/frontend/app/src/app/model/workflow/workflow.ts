import { TaskRequest, Connection } from './task-specification';

export interface Workflow {
	id: string | null;
	owned: boolean;
	name: string;
	description: string;
	steps: TaskRequest[];
	connections: Connection[];
	outputStep: TaskRequest | undefined;
}

export interface WorkflowDescription {
	id: string | null;
	name: string;
	description: string;
	owned: boolean;
}