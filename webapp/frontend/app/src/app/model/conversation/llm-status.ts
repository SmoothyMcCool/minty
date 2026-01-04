export interface LlmStatus {
	state: 'NOT_READY' | 'RUNNING' | 'COMPLETE',
	queuePosition: number
};