package tom.workflow.model;

public class TaskDescription extends Task {

	private String inputs = "";
	private String outputs = "";

	public TaskDescription() {
	}

	public String getInputs() {
		return inputs;
	}

	public void setInputs(String inputs) {
		this.inputs = inputs;
	}

	public String getOutputs() {
		return outputs;
	}

	public void setOutputs(String outputs) {
		this.outputs = outputs;
	}
}
