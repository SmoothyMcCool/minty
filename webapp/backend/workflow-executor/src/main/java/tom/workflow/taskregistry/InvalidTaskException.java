package tom.workflow.taskregistry;

public class InvalidTaskException extends Exception {

	private static final long serialVersionUID = 1399345187906304117L;

	public InvalidTaskException(String reason) {
		super(reason);
	}
}
