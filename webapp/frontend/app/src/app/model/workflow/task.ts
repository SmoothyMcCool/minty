import { AttributeMap } from "./task-specification";

export interface Task {
	name: string;
	configuration: AttributeMap;
}