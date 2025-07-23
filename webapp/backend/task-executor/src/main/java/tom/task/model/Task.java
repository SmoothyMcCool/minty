package tom.task.model;

import java.util.Map;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.task.converters.MapToStringConverter;

@Entity
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String template;
	private String name;
	private String description;
	@Convert(converter = MapToStringConverter.class)
	private Map<String, String> defaultConfig;
	private String outputTask;
	@Convert(converter = MapToStringConverter.class)
	private Map<String, String> outputTaskConfig;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, String> getDefaultConfig() {
		return defaultConfig;
	}

	public void setDefaultConfig(Map<String, String> defaultConfig) {
		this.defaultConfig = defaultConfig;
	}

	public String getOutputTask() {
		return outputTask;
	}

	public void setOutputTask(String outputTask) {
		this.outputTask = outputTask;
	}

	public Map<String, String> getOutputTaskConfig() {
		return outputTaskConfig;
	}

	public void setOutputTaskConfig(Map<String, String> outputTaskConfig) {
		this.outputTaskConfig = outputTaskConfig;
	}

}
