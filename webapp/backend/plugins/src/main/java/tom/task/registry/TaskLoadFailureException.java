package tom.task.registry;

public class TaskLoadFailureException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5830719296443725473L;

	public TaskLoadFailureException(String reason) {
		super(reason);
	}

	public TaskLoadFailureException(Throwable cause) {
		super(cause);
	}
}
