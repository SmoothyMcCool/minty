package tom.output.pug;

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

public class TemplateOutputHtmlFormatterConfig implements TaskConfigSpec {

	public static final String ResultTaskList = "Result Tasks";

	private String template;
	private List<String> resultTasks;

	public TemplateOutputHtmlFormatterConfig() {
		template = "";
	}

	public TemplateOutputHtmlFormatterConfig(Map<String, Object> config) throws DatabindException, JacksonException {
		this();
		if (config.containsKey(OutputTemplateHtmlSpecCreator.EnumName)) {
			template = config.get(OutputTemplateHtmlSpecCreator.EnumName).toString();
		}
		if (config.containsKey(TemplateOutputHtmlFormatterConfig.ResultTaskList)) {
			resultTasks = stringToList(config.get(TemplateOutputHtmlFormatterConfig.ResultTaskList).toString());
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(OutputTemplateHtmlSpecCreator.EnumName, TaskConfigTypes.EnumList);
		config.put(ResultTaskList, TaskConfigTypes.StringList);
		return config;
	}

	public String getTemplate() {
		return template;
	}

	public List<String> getResultTasks() {
		return resultTasks;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	@Override
	public Map<String, Object> getValues() {
		return Map.of(OutputTemplateHtmlSpecCreator.EnumName, template);
	}

	private List<String> stringToList(String listStr) throws DatabindException, JacksonException {
		ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;
		try {
			return mapper.readValue(listStr, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			throw new StringListFormatException("Could not read list of Task names", e);
		}

	}
}
