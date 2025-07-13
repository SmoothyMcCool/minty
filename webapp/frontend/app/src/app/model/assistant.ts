export enum AssistantState {
    READY = 'READY',
    PROCESSING_FILES = 'PROCESSING_FILES'
}

export interface Assistant {
    id: number;
    name: string;
    prompt: string;
    numFiles: number;
    state: AssistantState
    shared: boolean;
}