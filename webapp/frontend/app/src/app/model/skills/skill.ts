export interface SkillMetadata {
	name: string;
	description: string;
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