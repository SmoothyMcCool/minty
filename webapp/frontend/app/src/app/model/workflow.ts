import { Task } from "../model/task";

export interface Workflow {
    id: number;
    name: string;
    description: string;
    shared: boolean;
    workflowSteps: Task[];
    outputStep: Task;
}