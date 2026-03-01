export interface File {
	type: 'code' | 'markdown' | 'json' | 'text' | 'diagram';
	path: string;
	version: number;
	content: string;
}