package tom.assistant.service.agent.model;

import java.util.Map;

public class AgentStep {

	private String id;
	private AgentStepType type;
	private String name;
	private String worker;
	private Map<String, Object> input;
	private AgentResponseVisibility visibility;

	public AgentStep() {

	}

	public AgentStep(String id, AgentStepType type, String name, String worker, Map<String, Object> input,
			AgentResponseVisibility visibility) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.worker = worker;
		this.input = input;
		this.visibility = visibility;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AgentStepType getType() {
		return type;
	}

	public void setType(AgentStepType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getWorker() {
		return worker;
	}

	public void setWorker(String worker) {
		this.worker = worker;
	}

	public Map<String, Object> getInput() {
		return input;
	}

	public void setInput(Map<String, Object> input) {
		this.input = input;
	}

	public AgentResponseVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(AgentResponseVisibility visibility) {
		this.visibility = visibility;
	}

	@Override
	public String toString() {
		return "AgentStep [id=" + id + ", type=" + type + ", name=" + name + ", worker=" + worker + ", input=" + input
				+ ", visibility=" + visibility + "]";
	}

}