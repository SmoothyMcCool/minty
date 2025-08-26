package tom.workflow.tracking.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Convert;
import tom.workflow.converters.ExecutionStepStateToStringConverter;

public class ExecutionState {

	@Convert(converter = ExecutionStepStateToStringConverter.class)
	private List<ExecutionStepState> stepStates;

	public ExecutionState() {
		stepStates = new ArrayList<>();
	}

	public ExecutionState(int numSteps) {
		stepStates = new ArrayList<>();
		for (int i = 0; i < numSteps; i++) {
			stepStates.add(new ExecutionStepState());
		}
	}

	public List<ExecutionStepState> getStepStates() {
		return stepStates;
	}

	public void setStepStates(List<ExecutionStepState> stepStates) {
		this.stepStates = stepStates;
	}

}
