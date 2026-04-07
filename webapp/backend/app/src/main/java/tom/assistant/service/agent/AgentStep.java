package tom.assistant.service.agent;

import java.util.Map;

public class AgentStep {

	private String id;
	private String worker;
	private Map<String, Object> input;

	public AgentStep() {
		id = "";
		worker = "";
		input = null;
	}

	public AgentStep(String id, String worker, Map<String, Object> input) {
		super();
		this.id = id;
		this.worker = worker;
		this.input = input;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

}