package tom.workflow.tracking.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import tom.task.ExecutionResult;
import tom.workflow.converters.ExecutionResultConverter;

@Entity
@Table(name = "WorkflowRecord")
public class ExecutionRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Convert(converter = ExecutionResultConverter.class)
	private ExecutionResult result;

	private String output;
	private String outputFormat;

	public ExecutionRecord() {
		id = null;
		result = new ExecutionResult();
	}

	public ExecutionRecord(List<String> stepNames) {
		id = null;
		result = new ExecutionResult(stepNames);
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	@Transient
	public void addResult(String stepName, Map<String, Object> results) {
		result.addResult(stepName, results);
	}

	@Transient
	public void addStep(String stepName) {
		result.addStep(stepName);
	}

	@Transient
	public void addError(String stepName, String error) {
		result.addError(stepName, error);
	}
}
