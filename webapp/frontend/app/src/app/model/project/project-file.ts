export interface File {
	type: 'code' | 'markdown' | 'json' | 'yaml' | 'text' | 'diagram' | 'html';
	path: string;
	version: number;
	content: string;
}