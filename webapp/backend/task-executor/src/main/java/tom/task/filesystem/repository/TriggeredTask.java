package tom.task.filesystem.repository;

import java.util.Map;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.task.converters.MapToStringConverter;

@Entity
public class TriggeredTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String task;
	private String name;
	private String description;
	private String directory;
	@Convert(converter = MapToStringConverter.class)
	private Map<String, String> defaultConfig;

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

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public Map<String, String> getDefaultConfig() {
		return defaultConfig;
	}

	public void setDefaultConfig(Map<String, String> defaultConfig) {
		this.defaultConfig = defaultConfig;
	}
}
