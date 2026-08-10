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
		StepResult sr = new StepResult();
		sr.type = Type.SUCCESS;
		sr.response = response;
		return sr;
	}

	public static StepResult ask(LlmResponse response) {
		StepResult sr = new StepResult();
		sr.type = Type.ASK;
		sr.response = response;
		return sr;
	}

	public static StepResult ask(String question) {
		LlmResponse r = new LlmResponse();
		r.setMessage(question);

		StepResult sr = new StepResult();
		sr.type = Type.ASK;
		sr.response = r;
		return sr;
	}

	public static StepResult error(LlmResponse response) {
		StepResult sr = new StepResult();
		sr.type = Type.ERROR;
		sr.response = response;
		return sr;
	}

	public static StepResult error(String message) {
		LlmResponse r = new LlmResponse();
		r.setMessage(message);

		StepResult sr = new StepResult();
		sr.type = Type.ERROR;
		sr.response = r;
		return sr;
	}

	public static StepResult unstructured(String text) {
		StepResult sr = new StepResult();
		sr.type = Type.UNSTRUCTURED;
		sr.fallbackText = text;
		return sr;
	}

	public static StepResult replan(LlmResponse response) {
		StepResult sr = new StepResult();
		sr.type = Type.REPLAN;
		sr.response = response;
		sr.getResponse().setStatus(LlmStatus.SUCCESS); // To help out the replanner not replan the replan.
		return sr;
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
