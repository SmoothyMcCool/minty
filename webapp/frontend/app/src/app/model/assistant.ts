export interface Assistant {
	id: string;
	name: string;
	prompt: string;
	model: string;
	contextSize: number;
	temperature: number;
	topK: number;
	tools: string[];
	owned: boolean;
	hasMemory: boolean;
	documentIds: string[];
}

export function createAssistant(overrides: Partial<Assistant> = {}): Assistant {
	return {
		id: '',
		name: '',
		prompt: '',
		model: '',
		contextSize: 32768,
		temperature: 0,
		topK: 5,
		tools: [],
		owned: false,
		hasMemory: false,
		documentIds: [],
		...overrides
	};
}
