package tom.workflow.repository;

import java.util.Map;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.workflow.converters.MapToStringConverter;

@Entity
public class WorkflowTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String workflow;
	private String name;
	private String description;
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
	public String getWorkflow() {
		return workflow;
	}
	public void setWorkflow(String workflow) {
		this.workflow = workflow;
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
}
