package tom.workflow.tracking.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Convert;
import tom.workflow.converters.ExecutionStepStateToStringConverter;

public class ExecutionState {

	@Convert(converter = ExecutionStepStateToStringConverter.class)
	private Map<String, ExecutionStepState> stepStates;

	public ExecutionState() {
		stepStates = new HashMap<>();
	}

	public ExecutionState(List<String> stepNames) {
		this();
		for (String name : stepNames) {
			stepStates.put(name, new ExecutionStepState());
		}
	}

	public Map<String, ExecutionStepState> getStepStates() {
		return stepStates;
	}

	public void setStepStates(Map<String, ExecutionStepState> stepStates) {
		this.stepStates = stepStates;
	}

	public void addStep(String stepName) {
		stepStates.put(stepName, new ExecutionStepState());
	}
}
