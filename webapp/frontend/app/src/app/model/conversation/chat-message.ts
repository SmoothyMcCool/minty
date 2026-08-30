export const MessageTypes = [
	'USER',
	'ASSISTANT',
	'REASONING',
	'TOOL'
] as const;

export type MessageType =
	typeof MessageTypes[number];

export interface ChatMessage {
	id?: number;
	type: MessageType;
	message: string;
};