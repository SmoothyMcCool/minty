import { AttributeMap } from "./task-specification";

export interface PipelineDefinition {
	operations: PipelineOperation[];
}

export interface PipelineOperation {
	name: string;
	configuration: PipelineOperationConfiguration | null;
}

export type PipelineOperationConfiguration =
  | string
  | any[]
  | AttributeMap;
