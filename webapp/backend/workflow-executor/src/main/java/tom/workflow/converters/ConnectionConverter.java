package tom.workflow.converters;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.util.ClassConverter;
import tom.workflow.model.Connection;

@Converter
public class ConnectionConverter extends ClassConverter<Connection> {
	public ConnectionConverter() {
		super(new TypeReference<Connection>() {
		});
	}
}
