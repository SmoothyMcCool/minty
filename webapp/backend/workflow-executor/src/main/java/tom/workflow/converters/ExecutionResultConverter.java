package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.task.ExecutionResult;

@Converter
public class ExecutionResultConverter extends ClassConverter<ExecutionResult> {
	public ExecutionResultConverter() {
		super(new TypeReference<ExecutionResult>() {
		});
	}
}