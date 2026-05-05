package tom.tasks.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.MintyObjectMapper;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.transform.confluence.StringListFormatException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

public class SortConfig implements TaskConfigSpec {

	public static final String IdElement = "ID Element";

	private List<String> idElement;

	public SortConfig() {
		idElement = List.of();
	}

	public SortConfig(Map<String, Object> config) throws DatabindException, JacksonException {
		this();
		if (config.containsKey(IdElement)) {
			idElement = stringToList(config.get(IdElement).toString());
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(IdElement, TaskConfigTypes.StringList);
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

	public List<String> getIdElement() {
		return idElement;
	}

	public void setIdElement(List<String> idElement) {
		this.idElement = idElement;
	}

	@Override
	public Map<String, Object> getValues() {
		return Map.of(IdElement, idElement);
	}

	private List<String> stringToList(String pagesStr) throws DatabindException, JacksonException {
		ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;
		try {
			return mapper.readValue(pagesStr, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			throw new StringListFormatException("Could not read list of page IDs", e);
		}

	}
}
