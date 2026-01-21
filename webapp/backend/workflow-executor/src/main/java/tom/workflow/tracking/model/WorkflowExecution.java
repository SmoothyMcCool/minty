package tom.workflow.tracking.model;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import tom.api.UserId;
import tom.api.task.ExecutionResult;
import tom.api.task.Packet;
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
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "recordId", unique = true)
	private ExecutionRecord executionRecord;

	public WorkflowExecution() {
		id = null;
		this.ownerId = null;
		this.name = "";
		state = new ExecutionState();
		executionRecord = new ExecutionRecord();
	}

	public WorkflowExecution(List<String> stepNames, UserId ownerId) {
		this();
		id = null;
		this.ownerId = ownerId;
		state = new ExecutionState(stepNames);
		executionRecord = new ExecutionRecord(stepNames);
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

	public ExecutionRecord getExecutionRecord() {
		return executionRecord;
	}

	public void setExecutionRecord(ExecutionRecord executionRecord) {
		this.executionRecord = executionRecord;
	}

	public void completeTask(String stepName, Packet results, String error) {
		getExecutionRecord().addResult(stepName, results);
		getExecutionRecord().addError(stepName, error);

		state.getStepStates().get(stepName).completeTask();
		if (StringUtils.isNotBlank(error)) {
			state.getStepStates().get(stepName).failTask();
		}
	}

	public void addTasks(String stepName, int numTasks) {
		state.getStepStates().get(stepName).addTasks(numTasks);
	}

	public void addStep(String stepName) {
		state.addStep(stepName);
		executionRecord.addStep(stepName);
	}

	@Transient
	public ExecutionResult getResult() {
		return executionRecord != null ? executionRecord.getResult() : null;
	}
}
