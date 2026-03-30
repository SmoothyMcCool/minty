import { TaskRequest, Connection } from './task-specification';

export interface Workflow {
	id: string;
	owned: boolean;
	name: string;
	shared: boolean;
	description: string;
	steps: TaskRequest[];
	connections: Connection[];
	outputStep: TaskRequest;
}