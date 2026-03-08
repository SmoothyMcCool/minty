package tom.tasks.transform.pipeline.model;

import java.util.List;

public class PipelineDefinition {

	private List<PipelineOperation> operations;

	public PipelineDefinition() {
	}

	public List<PipelineOperation> getOperations() {
		return operations;
	}

	public void setOperations(List<PipelineOperation> operations) {
		this.operations = operations;
	}

}
