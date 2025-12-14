package tom.tasks.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class IdentifierConfig implements TaskConfigSpec {

	private final String IdElement = "ID Element";

	private String idElement;

	public IdentifierConfig() {
		idElement = "";
	}

	public IdentifierConfig(Map<String, String> config) {
		this();
		if (config.containsKey(IdElement)) {
			idElement = config.get(IdElement);
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(IdElement, TaskConfigTypes.Number);
		return config;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	public String getIdElement() {
		return idElement;
	}

	public void setIdElement(String idElement) {
		this.idElement = idElement;
	}

	@Override
	public Map<String, String> getValues() {
		return Map.of(IdElement, idElement);
	}
}
