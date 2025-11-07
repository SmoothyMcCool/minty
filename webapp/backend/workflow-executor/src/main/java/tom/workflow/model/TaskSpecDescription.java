package tom.workflow.model;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Convert;
import tom.task.TaskConfigTypes;
import tom.workflow.converters.StringListToStringConverter;
import tom.workflow.converters.StringMapToStringConverter;
import tom.workflow.converters.TaskConfigTypeMapToStringConverter;

public class TaskSpecDescription {

	private String taskName;
	private String group;
	@Convert(converter = StringMapToStringConverter.class)
	private Map<String, String> configuration;
	@Convert(converter = TaskConfigTypeMapToStringConverter.class)
	private Map<String, TaskConfigTypes> configSpec;
	@Convert(converter = StringListToStringConverter.class)
	private List<String> systemConfigVariables;
	@Convert(converter = StringListToStringConverter.class)
	private List<String> userConfigVariables;
	private String expects;
	private String produces;
	private int numInputs;
	private int numOutputs;

	public TaskSpecDescription() {
		taskName = "";
		configuration = Map.of();
		configSpec = Map.of();
		systemConfigVariables = List.of();
		userConfigVariables = List.of();
		expects = "";
		produces = "";
		numInputs = 0;
		numOutputs = 0;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

	public Map<String, TaskConfigTypes> getConfigSpec() {
		return configSpec;
	}

	public void setConfigSpec(Map<String, TaskConfigTypes> configSpec) {
		this.configSpec = configSpec;
	}

	public List<String> getSystemConfigVariables() {
		return systemConfigVariables;
	}

	public void setSystemConfigVariables(List<String> systemConfigVariables) {
		this.systemConfigVariables = systemConfigVariables;
	}

	public List<String> getUserConfigVariables() {
		return userConfigVariables;
	}

	public void setUserConfigVariables(List<String> userConfigVariables) {
		this.userConfigVariables = userConfigVariables;
	}

	public String getExpects() {
		return expects;
	}

	public void setExpects(String expects) {
		this.expects = expects;
	}

	public String getProduces() {
		return produces;
	}

	public void setProduces(String produces) {
		this.produces = produces;
	}

	public int getNumInputs() {
		return numInputs;
	}

	public void setNumInputs(int numInputs) {
		this.numInputs = numInputs;
	}

	public int getNumOutputs() {
		return numOutputs;
	}

	public void setNumOutputs(int numOutputs) {
		this.numOutputs = numOutputs;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

}
