package tom.workflow.converters;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;

@Converter
public class StringMapToStringConverter extends ClassConverter<Map<String, String>> {
	public StringMapToStringConverter() {
		super(new TypeReference<Map<String, String>>() {
		});
	}
}
