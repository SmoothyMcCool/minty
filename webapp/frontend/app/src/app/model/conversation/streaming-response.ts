import { LlmMetric } from "./llm-metric";
import { LlmStatus } from "./llm-status";

export const ChunkTypes = [
	'THINKING',
	'RESPONSE',
	'TOOL',
	'STATUS',
	'INTERNAL'
] as const;

export type ChunkType =
	typeof ChunkTypes[number];


export interface StreamingResponse {
	status: LlmStatus,
	metric: LlmMetric,
	sources: string[],
	type: ChunkType,
	content: string
}

export interface ThoughtEntry {
	type: 'THINKING' | 'TOOL';
	content: string;
}