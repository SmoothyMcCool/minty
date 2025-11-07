package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.workflow.tracking.model.ExecutionState;

@Converter
public class ExecutionStateConverter extends ClassConverter<ExecutionState> {
	public ExecutionStateConverter() {
		super(new TypeReference<ExecutionState>() {
		});
	}
}
