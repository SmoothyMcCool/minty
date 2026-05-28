export interface Conversation {
	id: string;
	title: string;
	ownerId: string;
	projectId: string;
	associatedAssistantId: string;
	lastUsed: number;
}