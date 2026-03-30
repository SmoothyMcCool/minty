export interface SkillMetadata {
	name: string;
	description: string;
	owned: boolean;
}

export interface SkillFile {
	relativePath: string;
	content: string;
}

export interface Skill {
	name: string;
	metadata: SkillMetadata;
	files: SkillFile[];
}