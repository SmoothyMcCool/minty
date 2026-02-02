package tom.tasks.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class IdentifyConfig implements TaskConfigSpec {

	public static final String IdElement = "ID Element";

	private String idElement;

	public IdentifyConfig() {
		idElement = "";
	}

	public IdentifyConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(IdElement)) {
			idElement = config.get(IdElement).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(IdElement, TaskConfigTypes.String);
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
	public Map<String, Object> getValues() {
		return Map.of(IdElement, idElement);
	}
}
