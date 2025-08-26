import { ChatMessage } from "../chat-message";

export interface WorkflowExecutionResult {
	startTime: Date;
	endTime: Date;
	results: Array<Array<Record<string, any>>>;
	errors: string[][];
	chatMessages: ChatMessage[];
}