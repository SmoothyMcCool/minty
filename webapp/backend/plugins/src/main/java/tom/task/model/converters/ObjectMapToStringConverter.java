package tom.task.model.converters;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.util.ClassConverter;

@Converter
public class ObjectMapToStringConverter extends ClassConverter<Map<String, Object>> {
	public ObjectMapToStringConverter() {
		super(new TypeReference<Map<String, Object>>() {
		});
	}
}
