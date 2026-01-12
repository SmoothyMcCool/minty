export interface OutputTaskSpecification {
	taskName: string;
	configuration: Map<string, string>;
	configSpec: Map<string, string>;
	systemConfigVariables: string[];
	userConfigVariables: string[];
}

export interface TaskSpecification {
	taskName: string;
	group: string;
	configuration: Map<string, string>;
	configSpec: Map<string, string>;
	systemConfigVariables: string[];
	userConfigVariables: string[];
	expects: string;
	produces: string;
	numInputs: number;
	numOutputs: number;
}

export interface TaskConfiguration {
	[key: string]: any;
}
export interface TaskRequest {
	taskName: string;
	stepName: string;
	id: string;
	configuration: TaskConfiguration;
	layout: TaskLayout
}

export interface Port {
	id: string;
	x: number;
	y: number;
	direction: 'input' | 'output';
}

export interface Block {
	id: string;
	x: number;
	y: number;
	width: number;
	height: number;
	inputs: Port[];
	outputs: Port[];
}

export interface Connection {
	readerId: string;
	readerPort: number;
	writerId: string;
	writerPort: number;
}

export interface TaskLayout {
	x: number;
	y: number;
	numInputs: number;
	numOutputs: number;
	tempX?: number;
	tempY?: number;
}