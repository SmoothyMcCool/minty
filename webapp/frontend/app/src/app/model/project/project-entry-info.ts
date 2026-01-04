export interface ProjectEntryInfo {
	id: string;
	type: 'folder' | 'reqts' | 'design' | 'story' | 'file' | 'unknown';
	name: string;
	parent: ProjectEntryInfo;
}