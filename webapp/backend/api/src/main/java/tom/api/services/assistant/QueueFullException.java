package tom.api.services.assistant;

public class QueueFullException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public QueueFullException(String reason) {
		super(reason);
	}
}
