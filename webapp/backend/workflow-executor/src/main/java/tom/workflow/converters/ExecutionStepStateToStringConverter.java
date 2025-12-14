package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.util.ClassConverter;
import tom.workflow.tracking.model.ExecutionStepState;

@Converter
public class ExecutionStepStateToStringConverter extends ClassConverter<ExecutionStepState> {
	public ExecutionStepStateToStringConverter() {
		super(new TypeReference<ExecutionStepState>() {
		});
	}
}
