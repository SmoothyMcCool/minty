package tom.workflow.converters;

import tools.jackson.core.type.TypeReference;

import jakarta.persistence.Converter;
import tom.api.services.workflow.Connection;
import tom.util.ClassConverter;

@Converter
public class ConnectionConverter extends ClassConverter<Connection> {
	public ConnectionConverter() {
		super(new TypeReference<Connection>() {
		});
	}
}
