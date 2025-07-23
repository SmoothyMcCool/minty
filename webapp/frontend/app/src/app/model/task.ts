export interface Task {
    id: number;
    template: string;
    name: string;
    description: string;
    watchLocation?: string;
    defaultConfig: Map<string, string>;
    outputTask: string,
    outputTaskConfig: Map<string, string>
}