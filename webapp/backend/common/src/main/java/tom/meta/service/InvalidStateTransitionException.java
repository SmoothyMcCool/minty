package tom.meta.service;

import tom.meta.model.LlmRequestId;
import tom.meta.model.RequestStatus;

/**
 * Thrown when a lifecycle transition is attempted from an invalid current
 * state.
 *
 * <p>
 * Lives in the domain library so any layer can catch it without a persistence
 * dependency.
 */
public class InvalidStateTransitionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -557788152956614106L;

	public InvalidStateTransitionException(LlmRequestId llmRequestId, RequestStatus currentStatus,
			String attemptedOperation) {
		super(String.format("Cannot perform '%s' on request %s - current status is %s", attemptedOperation,
				llmRequestId.value(), currentStatus));
	}

}