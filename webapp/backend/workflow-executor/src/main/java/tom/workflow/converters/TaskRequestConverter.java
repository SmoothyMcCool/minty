package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.workflow.model.TaskRequest;

@Converter
public class TaskRequestConverter extends ClassConverter<TaskRequest> {
	public TaskRequestConverter() {
		super(new TypeReference<TaskRequest>() {
		});
	}
}
