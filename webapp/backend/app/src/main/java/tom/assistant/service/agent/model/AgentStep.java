package tom.assistant.service.agent.model;

import java.util.Map;

public class AgentStep {

	private String id;
	private String name;
	private String worker;
	private Map<String, Object> input;

	public AgentStep() {
		id = "";
		name = "";
		worker = "";
		input = null;
	}

	public AgentStep(String id, String name, String worker, Map<String, Object> input) {
		super();
		this.id = id;
		this.name = name;
		this.worker = worker;
		this.input = input;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	@Override
	public String toString() {
		return "AgentStep [id=" + id + ", name=" + name + ", worker=" + worker + ", input=" + input + "]";
	}

}