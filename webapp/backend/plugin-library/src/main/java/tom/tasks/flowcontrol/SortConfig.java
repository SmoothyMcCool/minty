package tom.tasks.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.transform.confluence.StringListFormatException;

public class SortConfig implements TaskConfigSpec {

	public static final String IdElement = "ID Element";

	private List<String> idElement;

	public SortConfig() {
		idElement = List.of();
	}

	public SortConfig(Map<String, Object> config) throws JsonMappingException, JsonProcessingException {
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

	private List<String> stringToList(String pagesStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(pagesStr, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			throw new StringListFormatException("Could not read list of page IDs", e);
		}

	}
}
