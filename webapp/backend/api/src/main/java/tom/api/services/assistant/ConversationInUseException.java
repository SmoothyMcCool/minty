package tom.api.services.assistant;

public class ConversationInUseException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConversationInUseException(String reason) {
		super(reason);
	}
}
