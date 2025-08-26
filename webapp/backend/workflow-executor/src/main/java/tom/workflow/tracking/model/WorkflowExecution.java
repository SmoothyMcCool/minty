package tom.workflow.tracking.model;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.workflow.converters.ExecutionResultToStringConverter;
import tom.workflow.converters.ExecutionStateToStringConverter;

@Entity
public class WorkflowExecution {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UUID ownerId;
	private String name;
	@Convert(converter = ExecutionStateToStringConverter.class)
	private ExecutionState state;
	@Convert(converter = ExecutionResultToStringConverter.class)
	private ExecutionResult result;
	private String output;
	private String outputFormat;

	public WorkflowExecution() {
		id = null;
		this.ownerId = null;
		state = new ExecutionState();
		result = new ExecutionResult();
		output = "";
	}

	public WorkflowExecution(int numSteps, UUID ownerId) {
		id = null;
		this.ownerId = ownerId;
		state = new ExecutionState(numSteps);
		result = new ExecutionResult(numSteps);
		output = "";
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExecutionState getState() {
		return state;
	}

	public void setState(ExecutionState state) {
		this.state = state;
	}

	public ExecutionResult getResult() {
		return result;
	}

	public void setResult(ExecutionResult result) {
		this.result = result;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void completeTask(int step, Map<String, Object> results, String error) {
		getResult().addResult(step, results);
		getResult().addError(step, error);

		state.getStepStates().get(step).completeTask();
		if (error != null && !error.isBlank()) {
			state.getStepStates().get(step).failTask();
		}
	}

	public void addTasks(int step, int numTasks) {
		state.getStepStates().get(step).addTasks(numTasks);
	}

}
