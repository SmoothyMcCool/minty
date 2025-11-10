export interface Assistant {
	id: string;
	name: string;
	prompt: string;
	model: string;
	temperature: number;
	topK: number;
	ownerId: string;
	shared: boolean;
	hasMemory: boolean;
	documentIds: string[];
}
