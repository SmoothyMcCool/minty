export const ProjectNodeTypes = [
	'Folder',
	'File',
	'Conversation'
] as const;

export type ProjectNodeType =
	typeof ProjectNodeTypes[number];

export const ProjectFileTypes = [
	'code',
	'markdown',
	'json',
	'text',
	'diagram'
] as const;

export type ProjectFileType =
	typeof ProjectFileTypes[number];

export interface ProjectNode {
	type: ProjectNodeType;
	fileType?: ProjectFileType;
	path: string;
	version: number;
	content?: string;
}