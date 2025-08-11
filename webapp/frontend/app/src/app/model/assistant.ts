export interface Assistant {
	id: number;
	name: string;
	prompt: string;
	model: string;
	temperature: number;
	numFiles: number;
	ownerId: number;
	shared: boolean;
	hasMemory: boolean;
	documentIds: string[];
}
