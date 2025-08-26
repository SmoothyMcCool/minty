export interface Assistant {
	id: string;
	name: string;
	prompt: string;
	model: string;
	temperature: number;
	numFiles: number;
	ownerId: string;
	shared: boolean;
	hasMemory: boolean;
	documentIds: string[];
}
