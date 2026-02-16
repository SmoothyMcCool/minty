import { AttributeMap } from "./workflow/task-specification";

export interface User {
	id: string;
	name: string;
	password: string;
	defaults: AttributeMap;
	settings: AttributeMap;
}