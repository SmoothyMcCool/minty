package tom.workflow.converters;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;

@Converter
public class StringListConverter extends ClassConverter<List<List<String>>> {
	public StringListConverter() {
		super(new TypeReference<List<List<String>>>() {
		});
	}
}
