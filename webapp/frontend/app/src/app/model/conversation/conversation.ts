export interface Conversation {
	id: string;
	title: string;
	ownerId: string;
	associatedAssistantId: string;
	lastUsed: number;
}