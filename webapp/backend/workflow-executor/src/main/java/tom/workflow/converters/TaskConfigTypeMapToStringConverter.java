package tom.workflow.converters;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.task.TaskConfigTypes;

@Converter
public class TaskConfigTypeMapToStringConverter extends ClassConverter<Map<String, TaskConfigTypes>> {
	public TaskConfigTypeMapToStringConverter() {
		super(new TypeReference<Map<String, TaskConfigTypes>>() {
		});
	}
}
