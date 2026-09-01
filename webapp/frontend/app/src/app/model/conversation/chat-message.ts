export const MessageTypes = [
	'USER',
	'ASSISTANT',
	'REASONING',
	'TOOL',
	'SUBAGENT'
] as const;

export type MessageType =
	typeof MessageTypes[number];

export interface ChatMessage {
	id?: number;
	name: string;
	type: MessageType;
	message: string;
};