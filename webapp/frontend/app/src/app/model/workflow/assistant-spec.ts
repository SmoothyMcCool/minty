import { Assistant } from "../assistant";

export interface AssistantSpec {
	assistantId: string | null;
	assistant: Assistant | null;
}