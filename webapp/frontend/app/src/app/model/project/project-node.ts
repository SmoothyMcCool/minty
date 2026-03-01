export interface ProjectNode {
	type: 'Folder' | 'File';
	fileType?: 'code' | 'markdown' | 'json' | 'text' | 'diagram';
	path: string;
	version: number;
	content?: string;
}