package tom.assistant.service.agent.model;

public class AgentInputField {
	private String name;
	private String type;
	private boolean required;
	private String description;
	private AgentInputSource source;

	public AgentInputField(String name, Agent.InputField field) {
		this.name = name;
		this.type = field.type;
		this.required = field.required;
		this.source = field.source;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public AgentInputSource getSource() {
		return source;
	}

	public void setSource(AgentInputSource source) {
		this.source = source;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "AgentInputField [name=" + name + ", type=" + type + ", required=" + required + ", description="
				+ description + ", source=" + source + "]";
	}

}
