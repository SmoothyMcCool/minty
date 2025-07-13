export interface WorkflowTask {
    id: number;
    workflow: string;
    name: string;
    description: string;
    watchLocation?: string;
    defaultConfig: Map<string, string>;
}