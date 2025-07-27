import { Task } from "../model/task";

export interface StandaloneTask {
    id: number;
    ownerId: number;
    name: string;
    shared: boolean;
    triggered: boolean;
    watchLocation?: string;
    taskTemplate: Task;
    outputTemplate: Task;
}