export interface NodeInfo {
	nodeId: string;
	type: 'Folder' | 'File';
	name: string;
	parentId: string;
}