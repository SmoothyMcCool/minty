package tom.meta.service;

import tom.meta.model.LlmRequestId;

/**
 * Thrown when an operation references a request ID that does not exist.
 *
 * <p>
 * Lives in the domain library so any layer (web, messaging, etc.) can catch and
 * handle it without a persistence dependency.
 */
public class RequestNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1567834511217912333L;

	public RequestNotFoundException(LlmRequestId llmRequestId) {
		super("LLM Request not found: " + llmRequestId.value());
	}

}