package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.api.services.workflow.TaskRequest;
import tom.util.ClassConverter;

@Converter
public class TaskRequestConverter extends ClassConverter<TaskRequest> {
	public TaskRequestConverter() {
		super(new TypeReference<TaskRequest>() {
		});
	}
}
