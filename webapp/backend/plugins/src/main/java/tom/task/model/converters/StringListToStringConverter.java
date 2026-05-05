package tom.task.model.converters;

import java.util.List;

import tools.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.util.ClassConverter;

@Converter
public class StringListToStringConverter extends ClassConverter<List<String>> {
	public StringListToStringConverter() {
		super(new TypeReference<List<String>>() {
		});
	}
}
