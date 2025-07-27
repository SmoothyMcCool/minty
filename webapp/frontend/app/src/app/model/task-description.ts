export interface TaskDescription {
    name: string;
    configuration: Map<string, string>;
    inputs: string;
    outputs: string;
}