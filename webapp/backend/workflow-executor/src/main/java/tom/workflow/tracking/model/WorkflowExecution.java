package tom.workflow.tracking.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.api.UserId;
import tom.task.ExecutionResult;
import tom.workflow.converters.ExecutionResultConverter;
import tom.workflow.converters.ExecutionStateConverter;

@Entity
public class WorkflowExecution {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UserId ownerId;
	private String name;
	@Convert(converter = ExecutionStateConverter.class)
	private ExecutionState state;
	@Convert(converter = ExecutionResultConverter.class)
	private ExecutionResult result;
	private String output;
	private String outputFormat;

	public WorkflowExecution() {
		id = null;
		this.ownerId = null;
		this.name = "";
		state = new ExecutionState();
		result = new ExecutionResult();
		output = "";
		outputFormat = "";
	}

	public WorkflowExecution(List<String> stepNames, UserId ownerId) {
		this();
		id = null;
		this.ownerId = ownerId;
		state = new ExecutionState(stepNames);
		result = new ExecutionResult(stepNames);
		output = "";
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public void setOwnerId(UserId ownerId) {
		this.ownerId = ownerId;
	}

	public UserId getOwnerId() {
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

	public void completeTask(String stepName, Map<String, Object> results, String error) {
		getResult().addResult(stepName, results);
		getResult().addError(stepName, error);

		state.getStepStates().get(stepName).completeTask();
		if (error != null && !error.isBlank()) {
			state.getStepStates().get(stepName).failTask();
		}
	}

	public void addTasks(String stepName, int numTasks) {
		state.getStepStates().get(stepName).addTasks(numTasks);
	}

	public void addStep(String stepName) {
		state.addStep(stepName);
		result.addStep(stepName);
	}
}
