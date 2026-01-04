export interface User {
	id: string;
	name: string;
	password: string;
	defaults: Map<string, string>;
}