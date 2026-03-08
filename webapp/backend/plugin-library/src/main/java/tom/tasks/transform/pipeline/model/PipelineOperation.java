package tom.tasks.transform.pipeline.model;

public class PipelineOperation {

	private String name;
	private PipelineOperationConfiguration configuration;

	PipelineOperation() {
		this.name = "";
		this.configuration = null;
	}

	PipelineOperation(String name, PipelineOperationConfiguration configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PipelineOperationConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(PipelineOperationConfiguration configuration) {
		this.configuration = configuration;
	}

}
