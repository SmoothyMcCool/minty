package tom.task.model.converters;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.api.task.TaskConfigTypes;
import tom.util.ClassConverter;

@Converter
public class TaskConfigTypeMapToStringConverter extends ClassConverter<Map<String, TaskConfigTypes>> {
	public TaskConfigTypeMapToStringConverter() {
		super(new TypeReference<Map<String, TaskConfigTypes>>() {
		});
	}
}
