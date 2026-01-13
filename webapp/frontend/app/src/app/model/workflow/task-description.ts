import { AttributeMap } from "./task-specification";

export interface TaskDescription {
	name: string;
	configuration: AttributeMap;
	inputs: string;
	outputs: string;
}