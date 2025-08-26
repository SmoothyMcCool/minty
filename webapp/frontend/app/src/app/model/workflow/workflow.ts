import { Task } from '../task';

export interface Workflow {
	id: string;
	ownerId: string;
	name: string;
	description: string;
	shared: boolean;
	workflowSteps: Task[];
	outputStep: Task;
}