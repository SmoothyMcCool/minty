package tom.task.model;

import java.util.Map;

import jakarta.persistence.Convert;
import tom.task.converters.MapToStringConverter;

public class Task {

	private String name;
	@Convert(converter = MapToStringConverter.class)
	private Map<String, String> configuration;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

}
