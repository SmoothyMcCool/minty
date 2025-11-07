import { TaskRequest, Connection } from './task-specification';

export interface Workflow {
	id: string;
	ownerId: string;
	name: string;
	shared: boolean;
	description: string;
	steps: TaskRequest[];
	connections: Connection[];
	outputStep: TaskRequest;
}