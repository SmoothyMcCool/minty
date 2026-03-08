import { OutputTaskSpecification, TaskSpecification } from "./task-specification";

export interface PaletteNodeEntry {
	type: 'task' | 'output';
	item: TaskSpecification | OutputTaskSpecification;
}
