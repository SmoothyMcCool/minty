import { Task } from "../model/task";

export interface StandaloneTask {
    id: number;
    name: string;
    triggered: boolean;
    watchLocation?: string;
    taskTemplate: Task;
    outputTemplate: Task;
}