export enum DisplayMode {
	Fun = 'Fun',
	Boring = 'Boring'
}

export interface User {
	id: string;
	name: string;
	password: string;
	defaults: Map<string, string>;
	displayMode: DisplayMode;
}