package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.workflow.model.Task;

@Converter
public class TaskConverter extends ClassConverter<Task> {
	public TaskConverter() {
		super(new TypeReference<Task>() {
		});
	}
}
