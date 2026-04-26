package tom.assistant.service.agent.model;

import tom.assistant.service.agent.llm.LlmResponse;
import tom.assistant.service.agent.llm.LlmStatus;

public class StepResult {

	public enum Type {
		SUCCESS, ASK, REPLAN, ERROR, UNSTRUCTURED
	}

	private Type type;
	private LlmResponse response;
	private String fallbackText;

	public static StepResult success(LlmResponse response) {
		StepResult r = new StepResult();
		r.type = Type.SUCCESS;
		r.response = response;
		return r;
	}

	public static StepResult ask(LlmResponse response) {
		StepResult r = new StepResult();
		r.type = Type.ASK;
		r.response = response;
		return r;
	}

	public static StepResult ask(String question) {
		LlmResponse r = new LlmResponse();
		r.setMessage(question);

		StepResult s = new StepResult();
		s.type = Type.ASK;
		s.response = r;
		return s;
	}

	public static StepResult error(LlmResponse response) {
		StepResult r = new StepResult();
		r.type = Type.ERROR;
		r.response = response;
		return r;
	}

	public static StepResult error(String message) {
		LlmResponse r = new LlmResponse();
		r.setMessage(message);

		StepResult s = new StepResult();
		s.type = Type.ERROR;
		s.response = r;
		return s;
	}

	public static StepResult unstructured(String text) {
		StepResult r = new StepResult();
		r.type = Type.UNSTRUCTURED;
		r.fallbackText = text;
		return r;
	}

	public static StepResult replan(LlmResponse response) {
		StepResult r = new StepResult();
		r.type = Type.REPLAN;
		r.response = response;
		r.getResponse().setStatus(LlmStatus.SUCCESS); // To help out the replanner not replan the replan.
		return r;
	}

	public LlmResponse getResponse() {
		return response;
	}

	public Type getType() {
		return type;
	}

	public String getFallbackText() {
		return fallbackText;
	}

}
