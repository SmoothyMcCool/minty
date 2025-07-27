export enum AssistantState {
    READY = 'READY',
    PROCESSING_FILES = 'PROCESSING_FILES'
}

export interface Assistant {
    id: number;
    ownerId: number;
    name: string;
    prompt: string;
    model: string;
    temperature: number;
    numFiles: number;
    state: AssistantState
    shared: boolean;
}