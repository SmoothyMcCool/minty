package tom.assistant.service.agent.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import tom.api.AssistantId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;

public class Agent {

	private static final AssistantId GenericAgentId = new AssistantId(
			UUID.fromString("00000000-0000-0000-0000-000000000000"));

	private String name;
	private String purpose;

	private List<String> whenToUse;
	private List<String> whenNotToUse;

	private Inputs inputs;
	private String output;

	private String model;
	private Integer contextSize;
	private Double temperature;
	private Integer topK;
	private boolean hasMemory;
	private List<String> tools;
	private String prompt;
	private AgentResponseType responseType;

	public static class Inputs {
		@JsonAnySetter
		public Map<String, InputField> properties = new LinkedHashMap<>();

		@Override
		public String toString() {
			return "Inputs [properties=" + properties + "]";
		}
	}

	public static class InputField {
		public String type;
		public boolean required;
		public AgentInputSource source;
		public String description;

		@Override
		public String toString() {
			return "InputField [type=" + type + ", required=" + required + ", source=" + source + ", description="
					+ description + "]";
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public List<String> getWhenToUse() {
		return whenToUse;
	}

	public void setWhenToUse(List<String> whenToUse) {
		this.whenToUse = whenToUse;
	}

	public List<String> getWhenNotToUse() {
		return whenNotToUse;
	}

	public void setWhenNotToUse(List<String> whenNotToUse) {
		this.whenNotToUse = whenNotToUse;
	}

	public Inputs getInputs() {
		return inputs;
	}

	public void setInputs(Inputs inputs) {
		this.inputs = inputs;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Integer getContextSize() {
		return contextSize;
	}

	public void setContextSize(Integer contextSize) {
		this.contextSize = contextSize;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Integer getTopK() {
		return topK;
	}

	public void setTopK(Integer topK) {
		this.topK = topK;
	}

	public boolean isHasMemory() {
		return hasMemory;
	}

	public void setHasMemory(boolean hasMemory) {
		this.hasMemory = hasMemory;
	}

	public List<String> getTools() {
		return tools;
	}

	public void setTools(List<String> tools) {
		this.tools = tools;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	@Override
	public String toString() {
		return "Agent [name=" + name + ", purpose=" + purpose + ", whenToUse=" + whenToUse + ", whenNotToUse="
				+ whenNotToUse + ", inputs=" + inputs + ", output=" + output + ", model=" + model + ", contextSize="
				+ contextSize + ", temperature=" + temperature + ", topK=" + topK + ", hasMemory=" + hasMemory
				+ ", tools=" + tools + ", prompt=" + prompt + ", responseType=" + responseType + "]";
	}

	public AgentResponseType getResponseType() {
		return responseType;
	}

	public void setResponseType(AgentResponseType responseType) {
		this.responseType = responseType;
	}

	public Assistant toAssistant() {
		return new AssistantBuilder().contextSize(getContextSize()).hasMemory(isHasMemory()).model(getModel())
				.name(getName()).prompt(getPrompt()).temperature(getTemperature()).tools(getTools()).topK(getTopK())
				.id(GenericAgentId).documentIds(List.of()).build();
	}

}
