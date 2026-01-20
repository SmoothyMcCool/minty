package tom.task.registry;

public class InvalidTaskException extends RuntimeException {

	private static final long serialVersionUID = 5830719296443725473L;

	public InvalidTaskException(String reason) {
		super(reason);
	}

	public InvalidTaskException(Throwable cause) {
		super(cause);
	}
}
