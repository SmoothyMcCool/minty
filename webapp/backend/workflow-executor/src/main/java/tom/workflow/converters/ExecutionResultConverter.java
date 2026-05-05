package tom.workflow.converters;

import tools.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.api.task.ExecutionResult;
import tom.util.ClassConverter;

@Converter
public class ExecutionResultConverter extends ClassConverter<ExecutionResult> {
	public ExecutionResultConverter() {
		super(new TypeReference<ExecutionResult>() {
		});
	}
}