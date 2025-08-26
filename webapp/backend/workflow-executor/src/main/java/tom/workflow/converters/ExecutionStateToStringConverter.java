package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.workflow.tracking.model.ExecutionState;

@Converter
public class ExecutionStateToStringConverter extends ClassConverter<ExecutionState> {
	public ExecutionStateToStringConverter() {
		super(new TypeReference<ExecutionState>() {
		});
	}
}
