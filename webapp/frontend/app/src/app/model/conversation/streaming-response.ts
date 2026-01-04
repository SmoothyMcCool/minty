import { LlmMetric } from "./llm-metric";
import { LlmStatus } from "./llm-status";

export interface StreamingResponse {
	status: LlmStatus,
	metric: LlmMetric,
	sources: string[],
	content: String
}